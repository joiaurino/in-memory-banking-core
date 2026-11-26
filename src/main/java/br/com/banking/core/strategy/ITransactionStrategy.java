package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand;
import br.com.banking.core.enums.TransactionType;

import java.util.List;

public interface ITransactionStrategy<T extends TransactionCommand> {

    void validateTransaction(T command);

    List<Transaction> execute(T command);

    TransactionType supportedTransaction();
}