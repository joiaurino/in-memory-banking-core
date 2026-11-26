package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractTransactionStrategy<T extends TransactionCommand> implements ITransactionStrategy<T> {

    protected final Map<UUID, Deque<Transaction>> accountMap;

    protected AbstractTransactionStrategy(Map<UUID, Deque<Transaction>> accountMap) {
        if (accountMap == null) {
            throw new IllegalArgumentException("accountMap must not be null");
        }
        this.accountMap = accountMap;
    }

    protected Transaction incrementBalance(UUID transactionId, UUID accountId,
                                           BigDecimal amount, LocalDateTime timestamp,
                                           LocalDateTime scheduledDate, TransactionType type) {
        Deque<Transaction> transactions = accountMap.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        BigDecimal currentBalance = transactions.peek().balance();
        BigDecimal newBalance = currentBalance.add(amount);

        Transaction concludedTx = new Transaction(
                transactionId,
                accountId,
                amount,
                newBalance,
                timestamp,
                scheduledDate,
                type,
                TransactionStatus.CONCLUDED
        );

        transactions.push(concludedTx);
        return concludedTx;
    }

    protected Transaction decrementBalance(UUID transactionId, UUID accountId,
                                           BigDecimal amount, LocalDateTime timestamp,
                                           LocalDateTime scheduledDate, TransactionType type) {
        Deque<Transaction> transactions = accountMap.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist or has no transactions");
        }

        BigDecimal currentBalance = transactions.peek().balance();
        BigDecimal newBalance = currentBalance.subtract(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        Transaction concludedTx = new Transaction(
                transactionId,
                accountId,
                amount.negate(),
                newBalance,
                timestamp,
                scheduledDate,
                type,
                TransactionStatus.CONCLUDED
        );

        transactions.push(concludedTx);
        return concludedTx;
    }
}