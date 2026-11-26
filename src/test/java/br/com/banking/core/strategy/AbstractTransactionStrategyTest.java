package br.com.banking.core.strategy;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AbstractTransactionStrategyTest {

    private AbstractTransactionStrategy<DepositCommand> strategy;
    private Map<UUID, Deque<Transaction>> accountMap;
    private UUID accountId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        strategy = new AbstractTransactionStrategy<>() {
            @Override
            public TransactionType supportedTransaction() {
                return TransactionType.USER_DEPOSIT;
            }

            @Override
            public void validateTransaction(DepositCommand command, Map<UUID, Deque<Transaction>> map) {
            }

            @Override
            public List<Transaction> execute(DepositCommand command, Map<UUID, Deque<Transaction>> map) {
                return List.of();
            }
        };

        accountMap = new HashMap<>();
        accountId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 11, 25, 12, 0, 0);

        Transaction initial = new Transaction(
                UUID.randomUUID(), accountId, BigDecimal.ZERO, new BigDecimal("100.00"),
                now, null, TransactionType.CREATE_ACCOUNT, TransactionStatus.CONCLUDED
        );
        accountMap.put(accountId, new ArrayDeque<>(List.of(initial)));
    }

    @Test
    void shouldIncrementBalanceAndPushToLedger() {
        UUID txId = UUID.randomUUID();
        Transaction tx = strategy.incrementBalance(
                txId, accountId, new BigDecimal("50.00"), now.plusSeconds(5),
                null, TransactionType.USER_DEPOSIT, accountMap
        );

        assertEquals(txId, tx.transactionId());
        assertEquals(new BigDecimal("150.00"), tx.balance());
        assertEquals(new BigDecimal("50.00"), tx.amount());
        assertEquals(TransactionStatus.CONCLUDED, tx.transactionStatus());
        assertEquals(2, accountMap.get(accountId).size());
        assertEquals(tx, accountMap.get(accountId).peek());
    }

    @Test
    void shouldDecrementBalanceWithNegativeAmount() {
        UUID txId = UUID.randomUUID();
        Transaction tx = strategy.decrementBalance(
                txId, accountId, new BigDecimal("40.00"), now.plusSeconds(5),
                null, TransactionType.USER_WITHDRAWAL, accountMap
        );

        assertEquals(txId, tx.transactionId());
        assertEquals(new BigDecimal("60.00"), tx.balance());
        assertEquals(new BigDecimal("-40.00"), tx.amount());
        assertEquals(tx, accountMap.get(accountId).peek());
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        assertThrows(IllegalArgumentException.class, () ->
                strategy.decrementBalance(
                        UUID.randomUUID(), accountId, new BigDecimal("100.01"), now.plusSeconds(5),
                        null, TransactionType.USER_WITHDRAWAL, accountMap
                )
        );
    }

    @Test
    void shouldThrowWhenAccountDoesNotExistOrHasEmptyLedger() {
        UUID unknown = UUID.randomUUID();
        accountMap.put(unknown, new ArrayDeque<>());

        assertThrows(IllegalArgumentException.class, () ->
                strategy.incrementBalance(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, now, null, TransactionType.USER_DEPOSIT, accountMap)
        );
        assertThrows(IllegalArgumentException.class, () ->
                strategy.decrementBalance(UUID.randomUUID(), unknown, BigDecimal.TEN, now, null, TransactionType.USER_WITHDRAWAL, accountMap)
        );
    }
}