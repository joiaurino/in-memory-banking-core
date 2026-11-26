package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.util.*;

public class CreateAccountStrategy  extends AbstractTransactionStrategy<CreateAccountCommand>{

    public CreateAccountStrategy(Map<UUID, Deque<Transaction>> accountMap) {
        super(accountMap);
    }

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.CREATE_ACCOUNT;
    }

    @Override
    public void validateTransaction(CreateAccountCommand command) {
        if (command == null || command.accountId() == null || command.timestamp() == null) {
            throw new IllegalArgumentException("Command, accountId, and timestamp must not be null");
        }
        if (accountMap.containsKey(command.accountId())) {
            throw new IllegalArgumentException("Account already exists");
        }
    }

    @Override
    public List<Transaction> execute(CreateAccountCommand command) {
        validateTransaction(command);

        Transaction initialTx = new Transaction(
                UUID.randomUUID(),
                command.accountId(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                command.timestamp(),
                null,
                supportedTransaction(),
                TransactionStatus.CONCLUDED
        );

        accountMap.put(command.accountId(), new ArrayDeque<>(List.of(initialTx)));
        return List.of(initialTx);
    }
}
