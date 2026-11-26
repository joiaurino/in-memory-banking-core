package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CashbackPaymentCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class PayWithCashBackStrategy extends AbstractTransactionStrategy<CashbackPaymentCommand> {

    private static final BigDecimal CASHBACK_RATE = new BigDecimal("0.02");

    private final PriorityQueue<Transaction> cashbackHeap;
    private final Map<UUID, Transaction> cashbackIndex;

    public PayWithCashBackStrategy(PriorityQueue<Transaction> cashbackHeap,
                                   Map<UUID, Transaction> cashbackIndex,
                                   Map<UUID, Deque<Transaction>> accountMap) {

        super(accountMap);
        if (cashbackHeap == null || cashbackIndex == null) {
            throw new IllegalArgumentException("Cashback structures and account map must not be null");
        }
        this.cashbackHeap = cashbackHeap;
        this.cashbackIndex = cashbackIndex;
    }

    @Override
    public TransactionType supportedTransaction() {
        return TransactionType.CASHBACK_DEPOSIT;
    }

    @Override
    public void validateTransaction(CashbackPaymentCommand command) {
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
    public List<Transaction> execute(CashbackPaymentCommand command) {
        validateTransaction(command);
        Transaction paymentTransaction = decrementBalance(UUID.randomUUID(), command.accountId(), command.amount(),
                command.timestamp(), null, TransactionType.PAYMENT_TRANSFER);

        BigDecimal cashbackAmount = command.amount().multiply(CASHBACK_RATE).setScale(2, RoundingMode.DOWN);
        LocalDateTime scheduledDate = command.timestamp().plusSeconds(command.secondsUntilCashback());
        Transaction pendingCashback = new Transaction(UUID.randomUUID(), command.accountId(), cashbackAmount, null,
                command.timestamp(), scheduledDate, TransactionType.CASHBACK_DEPOSIT, TransactionStatus.PENDING);

        cashbackHeap.offer(pendingCashback);
        cashbackIndex.put(pendingCashback.transactionId(), pendingCashback);

        return List.of(paymentTransaction, pendingCashback);
    }

    public void processCashback(LocalDateTime currentTimestamp) {
        if (currentTimestamp == null) {
            throw new IllegalArgumentException("Current timestamp must not be null");
        }

        while (!cashbackHeap.isEmpty() && !cashbackHeap.peek().scheduledDate().isAfter(currentTimestamp)) {
            Transaction pending = cashbackHeap.poll();
            Transaction settled = incrementBalance(
                    pending.transactionId(),
                    pending.accountId(),
                    pending.amount(),
                    currentTimestamp,
                    pending.scheduledDate(),
                    TransactionType.CASHBACK_DEPOSIT
            );
            cashbackIndex.put(settled.transactionId(), settled);
        }
    }
}