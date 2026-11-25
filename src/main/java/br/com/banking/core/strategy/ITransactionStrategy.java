package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionType;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ITransactionStrategy <T extends TransactionCommand> {

    void validateTransaction(T command, Map<UUID, Deque<Transaction>> accountMap);

    List<Transaction> execute(T command, Map<UUID, Deque<Transaction>> accountMap);

    TransactionType supportedTransaction();
}
