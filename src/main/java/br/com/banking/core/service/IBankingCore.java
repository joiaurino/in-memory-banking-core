package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IBankingCore{

    <T extends TransactionCommand> List<Transaction> dispatch(T command);

    void processCashback(LocalDateTime currentTimestamp);

    TransactionStatus getCashbackStatus(UUID accountId, UUID transactionId);

    BigDecimal getBalanceAt(UUID accountId, LocalDateTime pointInTime);

    List<Transaction> getStatement(UUID accountId);

    List<Transaction> findTransactions(UUID accountId, TransactionType type, LocalDateTime from, LocalDateTime to);

}
