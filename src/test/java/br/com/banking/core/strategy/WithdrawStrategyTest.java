package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.domain.TransactionCommand.WithdrawCommand;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WithdrawStrategyTest {

    private WithdrawStrategy strategy;
    private UUID accountId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        Map<UUID, Deque<Transaction>> accountMap = new HashMap<>();
        strategy = new WithdrawStrategy(accountMap);
        accountId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);

        new CreateAccountStrategy(accountMap).execute(new CreateAccountCommand(accountId, now));
        new DepositStrategy(accountMap).execute(new DepositCommand(accountId, new BigDecimal("300.00"), now.plusSeconds(1)));
    }

    @Test
    void shouldSupportUserWithdrawalType() {
        assertEquals(TransactionType.USER_WITHDRAW, strategy.supportedTransaction());
    }

    @Test
    void shouldExecuteWithdrawAndDecreaseBalance() {
        List<Transaction> result = strategy.execute(
                new WithdrawCommand(accountId, new BigDecimal("120.00"), now.plusSeconds(5))
        );

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("180.00"), result.getFirst().balance());
        assertEquals(new BigDecimal("-120.00"), result.getFirst().amount());
    }

    @Test
    void shouldRejectInvalidAccountAmountOrOverdraft() {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new WithdrawCommand(UUID.randomUUID(), BigDecimal.TEN, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new WithdrawCommand(accountId, BigDecimal.ZERO, now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new WithdrawCommand(accountId, new BigDecimal("-1.00"), now)));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.execute(new WithdrawCommand(accountId, new BigDecimal("300.01"), now)));
    }
}