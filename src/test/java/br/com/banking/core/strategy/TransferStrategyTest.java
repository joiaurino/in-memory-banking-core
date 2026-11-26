package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.domain.TransactionCommand.TransferCommand;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TransferStrategyTest {

    private TransferStrategy strategy;
    private Map<UUID, Deque<Transaction>> accountMap;
    private UUID sourceId;
    private UUID targetId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        accountMap = new HashMap<>();
        sourceId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);

        CreateAccountStrategy createStrategy = new CreateAccountStrategy(accountMap);
        createStrategy.execute(new CreateAccountCommand(sourceId, now));
        createStrategy.execute(new CreateAccountCommand(targetId, now));
        new DepositStrategy(accountMap).execute(new DepositCommand(sourceId, new BigDecimal("500.00"), now.plusSeconds(1)));
        strategy = new TransferStrategy(accountMap);

    }

    @Test
    void shouldSupportTransferType() {
        assertEquals(TransactionType.TRANSFER, strategy.supportedTransaction());
    }

    @Test
    void shouldTransferFundsBetweenSourceAndTarget() {
        List<Transaction> result = strategy.execute(
                new TransferCommand(sourceId, targetId, new BigDecimal("200.00"), now.plusSeconds(5))
        );

        assertEquals(2, result.size());
        Transaction debit = result.get(0);
        Transaction credit = result.get(1);

        assertEquals(sourceId, debit.accountId());
        assertEquals(new BigDecimal("-200.00"), debit.amount());
        assertEquals(new BigDecimal("300.00"), debit.balance());

        assertEquals(targetId, credit.accountId());
        assertEquals(new BigDecimal("200.00"), credit.amount());
        assertEquals(new BigDecimal("200.00"), credit.balance());
    }

    @Test
    void shouldRejectSameAccountMissingAccountsOrInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new TransferCommand(sourceId, sourceId, BigDecimal.TEN, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new TransferCommand(null, targetId, BigDecimal.TEN, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new TransferCommand(sourceId, UUID.randomUUID(), BigDecimal.TEN, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new TransferCommand(sourceId, targetId, BigDecimal.ZERO, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new TransferCommand(sourceId, targetId, new BigDecimal("600.00"), now)));
    }

    @Test
    void shouldRollbackSourceDebitAtomicallyIfTargetCreditFails() {
        accountMap.put(targetId, new ArrayDeque<>());

        assertThrows(IllegalArgumentException.class, () ->
                strategy.execute(new TransferCommand(sourceId, targetId, new BigDecimal("200.00"), now.plusSeconds(5)))
        );

        assertEquals(2, accountMap.get(sourceId).size());
        assertEquals(new BigDecimal("500.00"), accountMap.get(sourceId).peek().balance());
    }
}