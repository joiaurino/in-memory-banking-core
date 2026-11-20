package br.com.banking.core.domain;

import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Transaction (
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        BigDecimal balance,
        LocalDateTime transactionDate,
        LocalDateTime scheduledDate,
        TransactionType transactionType,
        TransactionStatus transactionStatus){
}
