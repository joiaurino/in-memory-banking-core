package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.strategy.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class BankingCoreImpl implements IBankingCore {

    private final Map<UUID, Deque<Transaction>> accountMap;
    private final PriorityQueue<Transaction> cashbackHeap;
    private final Map<UUID, Transaction> cashbackIndex;
    private final Map<TransactionType, ITransactionStrategy<?>> strategies;
    private final PayWithCashBackStrategy cashBackStrategy;

    public BankingCoreImpl() {
        this.accountMap = new HashMap<>();
        this.cashbackHeap = new PriorityQueue<>(Comparator.comparing(Transaction::scheduledDate));
        this.cashbackIndex = new HashMap<>();
        this.cashBackStrategy = new PayWithCashBackStrategy(cashbackHeap, cashbackIndex, accountMap);
        this.strategies = Map.of(
                TransactionType.CREATE_ACCOUNT, new CreateAccountStrategy(accountMap),
                TransactionType.USER_DEPOSIT, new DepositStrategy(accountMap),
                TransactionType.USER_WITHDRAW, new WithdrawStrategy(accountMap),
                TransactionType.TRANSFER, new TransferStrategy(accountMap),
                TransactionType.CASHBACK_DEPOSIT, cashBackStrategy
        );
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

        List<Transaction> transactions = strategy.execute(command);

        return transactions;
    }

    @Override
    public void processCashback(LocalDateTime currentTimestamp) {
        cashBackStrategy.processCashback(currentTimestamp);
    }

    @Override
    public TransactionStatus getCashbackStatus(UUID accountId, UUID transactionId) {
        if (accountId == null || transactionId == null) {
            throw new IllegalArgumentException("AccountId and transactionId must not be null");
        }

        if (!accountMap.containsKey(accountId)) {
            throw new IllegalArgumentException("Account does not exist");
        }

        Transaction cashback = cashbackIndex.get(transactionId);
        if (cashback == null || !cashback.accountId().equals(accountId)) {
            throw new IllegalArgumentException("Cashback transaction not found");
        }

        return cashback.transactionStatus();
    }

    @Override
    public BigDecimal getBalanceAt(UUID accountId, LocalDateTime pointInTime) {

        if(accountId == null || pointInTime == null) {
            throw new IllegalArgumentException("AccountId and pointInTime must not be null");
        }

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
        Deque<Transaction> transactions = accountMap.get(accountId);
        if (transactions == null) {
            return List.of();
        }
        return List.copyOf(transactions);
    }

    @Override
    public List<Transaction> findTransactions(UUID accountId, TransactionType type, LocalDateTime from, LocalDateTime to) {
        final Deque<Transaction> transactions = accountMap.get(accountId);

        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        return transactions.stream()
                .takeWhile(t -> from == null || !t.transactionDate().isBefore(from))
                .filter(t -> type == null || t.transactionType() == type)
                .filter(t -> to == null || !t.transactionDate().isAfter(to))
                .toList();
    }
}