package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.domain.TransactionCommand.CashbackPaymentCommand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class PayWithCashBackStrategy extends AbstractTransactionStrategy<CashbackPaymentCommand> {

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.CASHBACK_DEPOSIT;
    }

    @Override
    public void validateTransaction(CashbackPaymentCommand command, Map<UUID, Deque<Transaction>> accountMap) {
        if (command == null || command.accountId() == null || command.timestamp() == null) {
            throw new IllegalArgumentException("Command, accountId, and timestamp must not be null");
        }
        if (!accountMap.containsKey(command.accountId())) {
            throw new IllegalArgumentException("Source account does not exist");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (command.secondsUntilCashback() < 0) {
            throw new IllegalArgumentException("Seconds until cashback cannot be negative");
        }
    }

    @Override
    public List<Transaction> execute(CashbackPaymentCommand command, Map<UUID, Deque<Transaction>> accountMap) {
        validateTransaction(command, accountMap);
        Transaction paymentTransaction = decrementBalance(command.accountId(), command.amount(),
                command.timestamp(), null, TransactionType.PAYMENT_TRANSFER, accountMap);

        BigDecimal cashbackAmount = command.amount().multiply(BigDecimal.valueOf(0.02)).setScale(2, RoundingMode.DOWN);
        final LocalDateTime scheduledDate = command.timestamp().plusSeconds(command.secondsUntilCashback());
        final Transaction pendingCashback = new Transaction(UUID.randomUUID(), command.accountId(), cashbackAmount, null,
                command.timestamp(), scheduledDate, TransactionType.CASHBACK_DEPOSIT, TransactionStatus.PENDING);

        return List.of(paymentTransaction, pendingCashback);
    }

    public void processCashback(LocalDateTime currentTimestamp, Map<UUID, Deque<Transaction>> accountMap, PriorityQueue<Transaction> cashbackHeap) {
        while (!cashbackHeap.isEmpty() && !cashbackHeap.peek().scheduledDate().isAfter(currentTimestamp)) {
            Transaction cashbackTransaction = cashbackHeap.poll();
            incrementBalance(cashbackTransaction.accountId(), cashbackTransaction.amount(), cashbackTransaction.scheduledDate(), null, TransactionType.CASHBACK_DEPOSIT, accountMap);
        }
    }
}
