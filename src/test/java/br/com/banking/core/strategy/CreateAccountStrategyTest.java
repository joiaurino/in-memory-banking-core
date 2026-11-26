package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateAccountStrategyTest {

    private CreateAccountStrategy strategy;
    private Map<UUID, Deque<Transaction>> accountMap;
    private UUID accountId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        strategy = new CreateAccountStrategy();
        accountMap = new HashMap<>();
        accountId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);
    }

    @Test
    void shouldSupportCreateAccountType() {
        assertEquals(TransactionType.CREATE_ACCOUNT, strategy.supportedTransaction());
    }

    @Test
    void shouldCreateAccountWithZeroBalance() {
        List<Transaction> result = strategy.execute(new CreateAccountCommand(accountId, now), accountMap);

        assertEquals(1, result.size());
        Transaction tx = result.getFirst();
        assertEquals(accountId, tx.accountId());
        assertEquals(BigDecimal.ZERO, tx.balance());
        assertEquals(TransactionStatus.CONCLUDED, tx.transactionStatus());
        assertTrue(accountMap.containsKey(accountId));
        assertEquals(1, accountMap.get(accountId).size());
    }

    @Test
    void shouldRejectNullArgumentsOrDuplicateAccount() {
        assertThrows(IllegalArgumentException.class, () -> strategy.execute(null, accountMap));
        assertThrows(IllegalArgumentException.class, () -> strategy.execute(new CreateAccountCommand(null, now), accountMap));
        assertThrows(IllegalArgumentException.class, () -> strategy.execute(new CreateAccountCommand(accountId, null), accountMap));

        strategy.execute(new CreateAccountCommand(accountId, now), accountMap);
        assertThrows(IllegalArgumentException.class, () -> strategy.execute(new CreateAccountCommand(accountId, now.plusSeconds(1)), accountMap));
    }
}