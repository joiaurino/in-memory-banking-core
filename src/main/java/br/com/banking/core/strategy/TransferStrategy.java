package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.domain.TransactionCommand.TransferCommand;

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
    public void validateTransaction(TransferCommand transaction, Map<UUID, Deque<Transaction>> accountMap) {


        if (transaction.sourceAccountId() == null || transaction.targetAccountId() == null ||
                transaction.sourceAccountId().equals(transaction.targetAccountId())) {
            throw new IllegalArgumentException("Source and target accounts must be valid and distinct");
        }
        if (!accountMap.containsKey(transaction.sourceAccountId()) || !accountMap.containsKey(transaction.targetAccountId())) {
            throw new IllegalArgumentException("Source or target account does not exist");
        }
        if (transaction.amount() == null || transaction.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public List<Transaction> execute(TransferCommand transaction, Map<UUID, Deque<Transaction>> accountMap) {

        validateTransaction(transaction, accountMap);

        UUID debitTransactionId = UUID.randomUUID();
        UUID creditTransactionId = UUID.randomUUID();
        Transaction debitTx = decrementBalance(debitTransactionId, transaction.sourceAccountId(), transaction.amount(), transaction.timestamp(), null, TransactionType.TRANSFER, accountMap);
        Transaction creditTx = incrementBalance(creditTransactionId, transaction.targetAccountId(), transaction.amount(), transaction.timestamp(), null, TransactionType.TRANSFER, accountMap);

        return List.of(debitTx, creditTx);
    }
}
