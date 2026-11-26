package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DepositStrategyTest {

    private DepositStrategy strategy;
    private UUID accountId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        Map<UUID, Deque<Transaction>> accountMap = new HashMap<>();
        accountId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);
        new CreateAccountStrategy(accountMap).execute(new CreateAccountCommand(accountId, now));
        strategy = new DepositStrategy(accountMap);

    }

    @Test
    void shouldSupportUserDepositType() {
        assertEquals(TransactionType.USER_DEPOSIT, strategy.supportedTransaction());
    }

    @Test
    void shouldExecuteDepositAndIncreaseBalance() {
        List<Transaction> result = strategy.execute(
                new DepositCommand(accountId, new BigDecimal("250.75"), now.plusSeconds(10))
        );

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("250.75"), result.getFirst().balance());
        assertEquals(TransactionType.USER_DEPOSIT, result.getFirst().transactionType());
    }

    @Test
    void shouldRejectNonExistentAccountOrNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new DepositCommand(UUID.randomUUID(), BigDecimal.TEN, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new DepositCommand(accountId, BigDecimal.ZERO, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new DepositCommand(accountId, new BigDecimal("-5.00"), now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new DepositCommand(accountId, null, now)));
    }
}