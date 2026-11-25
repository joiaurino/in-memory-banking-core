package br.com.banking.core.domain;

import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface TransactionCommand{
    TransactionType type();
    LocalDateTime timestamp();

    record CreateAccountCommand(UUID accountId, LocalDateTime timestamp) implements TransactionCommand {
        @Override
        public TransactionType type() {
            return TransactionType.CREATE_ACCOUNT;
        }
    }

    record DepositCommand(UUID accountId, BigDecimal amount, LocalDateTime timestamp) implements TransactionCommand {
        @Override public TransactionType type() { return TransactionType.USER_DEPOSIT; }
    }

    record WithdrawCommand(UUID accountId, BigDecimal amount, LocalDateTime timestamp) implements TransactionCommand {
        @Override public TransactionType type() { return TransactionType.USER_WITHDRAW; }
    }

    record TransferCommand(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, LocalDateTime timestamp) implements TransactionCommand {
        @Override public TransactionType type() { return TransactionType.TRANSFER; }
    }

    record CashbackPaymentCommand(UUID accountId, BigDecimal amount, LocalDateTime timestamp, long secondsUntilCashback) implements TransactionCommand {
        @Override public TransactionType type() { return TransactionType.CASHBACK_DEPOSIT;
        }
    }
}