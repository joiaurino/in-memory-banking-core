package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBankingCore {

    Transaction createAccount(UUID accountId, LocalDateTime timestamp);

    Transaction deposit(UUID accountId, BigDecimal amount, LocalDateTime timestamp);

    Transaction withdraw(UUID accountId, BigDecimal amount, LocalDateTime timestamp);

    Transaction transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, LocalDateTime timestamp);

    Transaction payWithCashback(UUID accountId, BigDecimal amount, LocalDateTime timestamp, long secondsUntilCashback);

    void processCashback(LocalDateTime currentTimestamp);

    TransactionStatus getCashbackStatus(UUID accountId, UUID transactionId);

    BigDecimal getBalanceAt(UUID accountId, LocalDateTime pointInTime);

    List<Transaction> getStatement(UUID accountId);

    List<Transaction> findTransactions(UUID accountId, TransactionType type, LocalDateTime from, LocalDateTime to);

}
