package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;


public class BankingCoreImpl implements IBankingCore{
    private final Map<UUID, Deque<Transaction>> accountMap;
    private final PriorityQueue<Transaction> cashbackHeap;

    public BankingCoreImpl() {
        this.accountMap = new HashMap<>();
        this.cashbackHeap = new PriorityQueue<>(Comparator.comparing(Transaction::scheduledDate));
    }

    @Override
    public Transaction createAccount(UUID accountId, LocalDateTime timestamp) {
        if (accountMap.containsKey(accountId)) {
            throw new IllegalArgumentException("Account already exists");
        }
        final Transaction transaction = new Transaction(UUID.randomUUID(), accountId, BigDecimal.ZERO,
                BigDecimal.ZERO, timestamp, null,
                TransactionType.CREATE_ACCOUNT, TransactionStatus.CONCLUDED);

        accountMap.put(accountId, new ArrayDeque<>(List.of(transaction)));
        return transaction;
    }

    @Override
    public Transaction deposit(UUID accountId, BigDecimal amount, LocalDateTime timestamp) {
        return incrementBalanceAccount(accountId, amount, timestamp, null, TransactionType.USER_DEPOSIT);
    }

    @Override
    public Transaction withdraw(UUID accountId, BigDecimal amount, LocalDateTime timestamp) {
        return decrementBalanceAccount(accountId, amount, timestamp, null, TransactionType.USER_WITHDRAW);
    }

    @Override
    public Transaction transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, LocalDateTime timestamp) {
        if(!accountMap.containsKey(sourceAccountId) || !accountMap.containsKey(targetAccountId)
                || sourceAccountId.equals(targetAccountId) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid transfer parameters");
        }

        Transaction debitTransaction = decrementBalanceAccount(sourceAccountId, amount, timestamp, null, TransactionType.TRANSFER);
        incrementBalanceAccount(targetAccountId, amount, timestamp, null, TransactionType.TRANSFER);
        return debitTransaction;
    }

    @Override
    public Transaction payWithCashback(UUID accountId, BigDecimal amount, LocalDateTime timestamp, long secondsUntilCashback) {
        decrementBalanceAccount(accountId, amount, timestamp, null, TransactionType.PAYMENT_TRANSFER);
        BigDecimal cashbackAmount = amount.multiply(BigDecimal.valueOf(0.02)).setScale(2, RoundingMode.DOWN);;
        final LocalDateTime scheduledDate = timestamp.plusSeconds(secondsUntilCashback);
        final Transaction pendingCashback = new Transaction(UUID.randomUUID(), accountId, cashbackAmount, null,
                timestamp, scheduledDate, TransactionType.CASHBACK_DEPOSIT, TransactionStatus.PENDING);

        cashbackHeap.offer(pendingCashback);
        return pendingCashback;
    }

    @Override
    public void processCashback(LocalDateTime currentTimestamp) {
        while (!cashbackHeap.isEmpty() && !cashbackHeap.peek().scheduledDate().isAfter(currentTimestamp)) {
            Transaction cashbackTransaction = cashbackHeap.poll();
            incrementBalanceAccount(cashbackTransaction.accountId(), cashbackTransaction.amount(), cashbackTransaction.scheduledDate(), null, TransactionType.CASHBACK_DEPOSIT);
        }
    }

    @Override
    public TransactionStatus getCashbackStatus(UUID accountId, UUID transactionId) {
        return cashbackHeap.stream()
                .filter(t -> t.accountId().equals(accountId) && t.transactionId().equals(transactionId))
                .findFirst()
                .map(Transaction::transactionStatus)
                .orElseThrow();
    }

    @Override
    public BigDecimal getBalanceAt(UUID accountId, LocalDateTime pointInTime) {

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
        return Optional.ofNullable(accountMap.get(accountId)).orElse(new ArrayDeque<>()).stream().toList();
    }

    @Override
    public List<Transaction> findTransactions(UUID accountId, TransactionType type, LocalDateTime from, LocalDateTime to) {
        final Deque<Transaction> transactions = accountMap.get(accountId);

        if(transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        return transactions.stream()
                .filter(t -> type == null || t.transactionType() == type)
                .filter(t -> from == null || !t.transactionDate().isBefore(from))
                .filter(t -> to == null || !t.transactionDate().isAfter(to))
                .toList();
    }

    private Transaction incrementBalanceAccount(UUID accountId, BigDecimal amount, LocalDateTime timestamp, LocalDateTime scheduledDate, TransactionType transactionType) {
        validateOperation(accountId, amount);
        final Transaction currentTransaction = accountMap.get(accountId).peek();

        final BigDecimal currentBalance = currentTransaction.balance();
        final BigDecimal newBalance = currentBalance.add(amount);
        final Transaction newIncrementTransaction = new Transaction(UUID.randomUUID(), accountId, amount, newBalance, timestamp, scheduledDate, transactionType, TransactionStatus.CONCLUDED);
        accountMap.get(accountId).push(newIncrementTransaction);
        return newIncrementTransaction;
    }

    private Transaction decrementBalanceAccount(UUID accountId, BigDecimal amount, LocalDateTime timestamp, LocalDateTime scheduledDate, TransactionType transactionType) {
        validateOperation(accountId, amount);
        final Transaction currentTransaction = accountMap.get(accountId).peek();
        final BigDecimal newBalance = currentTransaction.balance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        final Transaction newDebitTransaction = new Transaction(UUID.randomUUID(), accountId, amount.negate(), newBalance, timestamp, scheduledDate, transactionType, TransactionStatus.CONCLUDED);
        accountMap.get(accountId).push(newDebitTransaction);
        return newDebitTransaction;
    }

    private void validateOperation(UUID accountId, BigDecimal amount) {
        if (!accountMap.containsKey(accountId)) {
            throw new IllegalArgumentException("Account does not exist");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }


}

