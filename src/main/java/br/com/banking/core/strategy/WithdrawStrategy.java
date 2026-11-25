package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.domain.TransactionCommand.WithdrawCommand;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WithdrawStrategy extends AbstractTransactionStrategy<WithdrawCommand> {

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.USER_WITHDRAWAL;
    }

    @Override
    public void validateTransaction(WithdrawCommand command, Map<UUID, Deque<Transaction>> accountMap) {

        if (!accountMap.containsKey(command.accountId())) {
            throw new IllegalArgumentException("Source account does not exist");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public List<Transaction> execute(WithdrawCommand command, Map<UUID, Deque<Transaction>> accountMap) {
        validateTransaction(command, accountMap);

        return List.of(decrementBalance(
                command.accountId(),
                command.amount(),
                command.timestamp(),
                null,
                supportedTransaction(),
                accountMap));
    }
}
