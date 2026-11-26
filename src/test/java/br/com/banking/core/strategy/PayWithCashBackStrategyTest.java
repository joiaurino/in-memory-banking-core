package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CashbackPaymentCommand;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PayWithCashBackStrategyTest {

    private PayWithCashBackStrategy strategy;
    private Map<UUID, Deque<Transaction>> accountMap;
    private PriorityQueue<Transaction> cashbackHeap;
    private Map<UUID, Transaction> cashbackIndex;
    private UUID accountId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        accountMap = new HashMap<>();
        cashbackHeap = new PriorityQueue<>(Comparator.comparing(Transaction::scheduledDate));
        cashbackIndex = new HashMap<>();
        accountId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);

        new CreateAccountStrategy(accountMap).execute(new CreateAccountCommand(accountId, now));
        new DepositStrategy(accountMap).execute(new DepositCommand(accountId, new BigDecimal("500.00"), now.plusSeconds(1)));

        strategy = new PayWithCashBackStrategy(cashbackHeap, cashbackIndex, accountMap);
    }

    @Test
    void shouldSupportCashbackDepositType() {
        assertEquals(TransactionType.CASHBACK_DEPOSIT, strategy.supportedTransaction());
    }

    @Test
    void shouldDebitPaymentAndScheduleTwoPercentCashbackRoundingDown() {
        List<Transaction> result = strategy.execute(
                new CashbackPaymentCommand(accountId, new BigDecimal("99.99"), now.plusSeconds(5), 20)
        );

        assertEquals(2, result.size());
        Transaction payment = result.get(0);
        Transaction pendingCashback = result.get(1);

        assertEquals(TransactionType.PAYMENT_TRANSFER, payment.transactionType());
        assertEquals(new BigDecimal("400.01"), payment.balance());

        assertEquals(TransactionType.CASHBACK_DEPOSIT, pendingCashback.transactionType());
        assertEquals(TransactionStatus.PENDING, pendingCashback.transactionStatus());
        assertEquals(new BigDecimal("1.99"), pendingCashback.amount());
        assertEquals(now.plusSeconds(25), pendingCashback.scheduledDate());
    }

    @Test
    void shouldProcessDueCashbacksPreservingTransactionIdAndUpdatingIndex() {
        List<Transaction> result = strategy.execute(
                new CashbackPaymentCommand(accountId, new BigDecimal("200.00"), now.plusSeconds(5), 15)
        );

        Transaction pendingCashback = result.get(1);
        assertEquals(TransactionStatus.PENDING, cashbackIndex.get(pendingCashback.transactionId()).transactionStatus());

        strategy.processCashback(now.plusSeconds(19));
        assertEquals(1, cashbackHeap.size());
        assertEquals(new BigDecimal("300.00"), accountMap.get(accountId).peek().balance());
        assertEquals(TransactionStatus.PENDING, cashbackIndex.get(pendingCashback.transactionId()).transactionStatus());

        strategy.processCashback(now.plusSeconds(20));
        assertTrue(cashbackHeap.isEmpty());

        Transaction settled = accountMap.get(accountId).peek();
        assertEquals(pendingCashback.transactionId(), settled.transactionId());
        assertEquals(TransactionStatus.CONCLUDED, settled.transactionStatus());
        assertEquals(new BigDecimal("304.00"), settled.balance());
        assertEquals(TransactionStatus.CONCLUDED, cashbackIndex.get(pendingCashback.transactionId()).transactionStatus());
    }

    @Test
    void shouldRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> strategy.execute(null));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new CashbackPaymentCommand(UUID.randomUUID(), BigDecimal.TEN, now, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new CashbackPaymentCommand(accountId, BigDecimal.ZERO, now, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new CashbackPaymentCommand(accountId, BigDecimal.TEN, now, -1)));
    }
}