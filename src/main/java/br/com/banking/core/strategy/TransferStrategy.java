package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.TransferCommand;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransferStrategy extends AbstractTransactionStrategy<TransferCommand> {

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.TRANSFER;
    }

    @Override
    public void validateTransaction(TransferCommand command, Map<UUID, Deque<Transaction>> accountMap) {

        if (command == null || command.timestamp() == null) {
            throw new IllegalArgumentException("Command and timestamp must not be null");
        }
        if (command.sourceAccountId() == null || command.targetAccountId() == null ||
                command.sourceAccountId().equals(command.targetAccountId())) {
            throw new IllegalArgumentException("Source and target accounts must be valid and distinct");
        }
        if (!accountMap.containsKey(command.sourceAccountId()) || !accountMap.containsKey(command.targetAccountId())) {
            throw new IllegalArgumentException("Source or target account does not exist");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public List<Transaction> execute(TransferCommand command, Map<UUID, Deque<Transaction>> accountMap) {
        validateTransaction(command, accountMap);

        Transaction debitTx = decrementBalance(UUID.randomUUID(), command.sourceAccountId(), command.amount(), command.timestamp(), null, supportedTransaction(), accountMap);
        Transaction creditTx = incrementBalance(UUID.randomUUID(), command.targetAccountId(), command.amount(), command.timestamp(), null, supportedTransaction(), accountMap);

        return List.of(debitTx, creditTx);
    }
}