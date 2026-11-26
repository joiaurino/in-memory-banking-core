package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.strategy.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BankingCoreImpl implements IBankingCore {

    private final Map<UUID, Deque<Transaction>> accountMap;
    private final PriorityQueue<Transaction> cashbackHeap;
    private final Map<TransactionType, ITransactionStrategy<?>> strategies;
    private final PayWithCashBackStrategy cashBackStrategy;

    public BankingCoreImpl() {
        this.accountMap = new HashMap<>();
        this.cashbackHeap = new PriorityQueue<>(Comparator.comparing(Transaction::scheduledDate));
        this.cashBackStrategy = new PayWithCashBackStrategy();
        this.strategies = Stream.of(
                new CreateAccountStrategy(),
                new DepositStrategy(),
                new WithdrawStrategy(),
                new TransferStrategy(),
                cashBackStrategy
        ).collect(Collectors.toUnmodifiableMap(
                ITransactionStrategy::supportedTransaction,
                Function.identity()
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TransactionCommand> List<Transaction> dispatch(T command) {
        if (command == null || command.timestamp() == null) {
            throw new IllegalArgumentException("Command and timestamp must not be null");
        }

        ITransactionStrategy<T> strategy = (ITransactionStrategy<T>) strategies.get(command.type());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported transaction type: " + command.type());
        }

        processCashback(command.timestamp());

        List<Transaction> transactions = strategy.execute(command, accountMap);

        if (command.type() == TransactionType.CASHBACK_DEPOSIT) {
            cashbackHeap.offer(transactions.stream()
                    .filter(t -> t.transactionType() == TransactionType.CASHBACK_DEPOSIT
                            && t.transactionStatus() == TransactionStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No pending cashback transaction found")));
        }

        return transactions;
    }

    @Override
    public void processCashback(LocalDateTime currentTimestamp) {
        cashBackStrategy.processCashback(currentTimestamp, accountMap, cashbackHeap);
    }

    @Override
    public TransactionStatus getCashbackStatus(UUID accountId, UUID transactionId) {
        if (!accountMap.containsKey(accountId)) {
            throw new IllegalArgumentException("Account does not exist");
        }

        return cashbackHeap.stream()
                .filter(t -> t.accountId().equals(accountId)
                        && t.transactionId().equals(transactionId))
                .findFirst()
                .map(Transaction::transactionStatus)
                .orElseGet(() -> accountMap.get(accountId).stream()
                        .filter(t -> t.transactionId().equals(transactionId))
                        .findFirst()
                        .map(Transaction::transactionStatus)
                        .orElseThrow(() -> new IllegalArgumentException("Cashback transaction not found")));
    }

    @Override
    public BigDecimal getBalanceAt(UUID accountId, LocalDateTime pointInTime) {
        processCashback(pointInTime);

        final Deque<Transaction> transactions = accountMap.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        return transactions.stream()
                .filter(t -> !t.transactionDate().isAfter(pointInTime))
                .findFirst()
                .map(Transaction::balance)
                .orElseThrow(() -> new IllegalArgumentException("No transactions found before the specified point in time"));
    }

    @Override
    public List<Transaction> getStatement(UUID accountId) {
        return Optional.ofNullable(accountMap.get(accountId))
                .orElse(new ArrayDeque<>())
                .stream()
                .toList();
    }

    @Override
    public List<Transaction> findTransactions(UUID accountId, TransactionType type, LocalDateTime from, LocalDateTime to) {
        final Deque<Transaction> transactions = accountMap.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        return transactions.stream()
                .filter(t -> type == null || t.transactionType() == type)
                .filter(t -> from == null || !t.transactionDate().isBefore(from))
                .filter(t -> to == null || !t.transactionDate().isAfter(to))
                .toList();
    }
}