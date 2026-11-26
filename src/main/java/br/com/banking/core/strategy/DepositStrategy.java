package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DepositStrategy extends AbstractTransactionStrategy<DepositCommand>{

    public DepositStrategy(Map<UUID, Deque<Transaction>> accountMap) {
        super(accountMap);
    }

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.USER_DEPOSIT;
    }

    @Override
    public void validateTransaction(DepositCommand command) {
        if (command == null || command.accountId() == null || command.timestamp() == null) {
            throw new IllegalArgumentException("Command, accountId, and timestamp must not be null");
        }
        if (!accountMap.containsKey(command.accountId())) {
            throw new IllegalArgumentException("Source account does not exist");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public List<Transaction> execute(DepositCommand command) {
        validateTransaction(command);

        return List.of(incrementBalance(
                UUID.randomUUID(),
                command.accountId(),
                command.amount(),
                command.timestamp(),
                null,
                supportedTransaction())
        );
    }
}
