package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;
import br.com.banking.core.domain.TransactionCommand.DepositCommand;
import br.com.banking.core.domain.TransactionCommand.WithdrawCommand;
import br.com.banking.core.domain.TransactionCommand.TransferCommand;
import br.com.banking.core.domain.TransactionCommand.CashbackPaymentCommand;

import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankingCoreImplTest {

    private IBankingCore bankingCore;
    private UUID accountA;
    private UUID accountB;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        bankingCore = new BankingCoreImpl();
        accountA = UUID.randomUUID();
        accountB = UUID.randomUUID();
        baseTime = LocalDateTime.of(2026, 11, 25, 10, 0, 0);
    }

    @Nested
    @DisplayName("Account Creation & Dispatch Validation")
    class AccountAndDispatchTests {

        @Test
        @DisplayName("Should create account with zero balance and CONCLUDED status")
        void shouldCreateAccountSuccessfully() {
            List<Transaction> result = bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime));

            assertEquals(1, result.size());
            Transaction tx = result.getFirst();
            assertEquals(accountA, tx.accountId());
            assertEquals(BigDecimal.ZERO, tx.balance());
            assertEquals(TransactionType.CREATE_ACCOUNT, tx.transactionType());
            assertEquals(TransactionStatus.CONCLUDED, tx.transactionStatus());
        }

        @Test
        @DisplayName("Should throw when creating duplicate account")
        void shouldThrowWhenAccountAlreadyExists() {
            bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime));

            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime.plusSeconds(1))));
        }

        @Test
        @DisplayName("Should throw when command or timestamp is null")
        void shouldThrowWhenCommandOrTimestampIsNull() {
            assertThrows(IllegalArgumentException.class, () -> bankingCore.dispatch(null));
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new CreateAccountCommand(accountA, null)));
        }
    }

    @Nested
    @DisplayName("Deposit, Withdraw & Transfer Strategies")
    class StandardTransactionsTests {

        @BeforeEach
        void initAccounts() {
            bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime));
            bankingCore.dispatch(new CreateAccountCommand(accountB, baseTime));
        }

        @Test
        @DisplayName("Should deposit and update balance correctly")
        void shouldDepositSuccessfully() {
            Transaction tx = bankingCore.dispatch(
                    new DepositCommand(accountA, new BigDecimal("500.00"), baseTime.plusSeconds(10))
            ).getFirst();

            assertEquals(new BigDecimal("500.00"), tx.balance());
            assertEquals(TransactionType.USER_DEPOSIT, tx.transactionType());
        }

        @Test
        @DisplayName("Should reject non-positive deposit or non-existent account")
        void shouldRejectInvalidDeposit() {
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new DepositCommand(accountA, BigDecimal.ZERO, baseTime.plusSeconds(1))));
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("-10.00"), baseTime.plusSeconds(1))));
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new DepositCommand(UUID.randomUUID(), new BigDecimal("100.00"), baseTime.plusSeconds(1))));
        }

        @Test
        @DisplayName("Should withdraw when balance is sufficient and block overdraft")
        void shouldWithdrawAndValidateInsufficientFunds() {
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("200.00"), baseTime.plusSeconds(5)));

            Transaction withdrawTx = bankingCore.dispatch(
                    new WithdrawCommand(accountA, new BigDecimal("75.50"), baseTime.plusSeconds(10))
            ).getFirst();

            assertEquals(new BigDecimal("124.50"), withdrawTx.balance());
            assertEquals(new BigDecimal("-75.50"), withdrawTx.amount());
            assertEquals(TransactionType.USER_WITHDRAW, withdrawTx.transactionType());

            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new WithdrawCommand(accountA, new BigDecimal("125.00"), baseTime.plusSeconds(15))));
        }

        @Test
        @DisplayName("Should transfer atomically between distinct accounts")
        void shouldTransferBetweenAccounts() {
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("1000.00"), baseTime.plusSeconds(5)));

            List<Transaction> txs = bankingCore.dispatch(
                    new TransferCommand(accountA, accountB, new BigDecimal("350.00"), baseTime.plusSeconds(10))
            );

            assertEquals(2, txs.size());
            assertEquals(new BigDecimal("650.00"), txs.get(0).balance());
            assertEquals(new BigDecimal("350.00"), txs.get(1).balance());
        }

        @Test
        @DisplayName("Should block self-transfer")
        void shouldBlockSelfTransfer() {
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("100.00"), baseTime.plusSeconds(5)));

            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new TransferCommand(accountA, accountA, new BigDecimal("50.00"), baseTime.plusSeconds(10))));
        }
    }

    @Nested
    @DisplayName("Cashback Lifecycle & PriorityQueue Processing")
    class CashbackTests {

        @BeforeEach
        void initAccountWithFunds() {
            bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime));
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("1000.00"), baseTime.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should schedule 2% cashback as PENDING and settle as CONCLUDED when due")
        void shouldProcessCashbackLifecycle() {
            List<Transaction> result = bankingCore.dispatch(
                    new CashbackPaymentCommand(accountA, new BigDecimal("250.00"), baseTime.plusSeconds(10), 30)
            );

            assertEquals(2, result.size());
            Transaction paymentTx = result.get(0);
            Transaction cashbackTx = result.get(1);

            assertEquals(new BigDecimal("750.00"), paymentTx.balance());
            assertEquals(new BigDecimal("5.00"), cashbackTx.amount());
            assertEquals(TransactionStatus.PENDING, cashbackTx.transactionStatus());

            // Antes do vencimento (t + 25s): continua PENDING e saldo é 750.00
            assertEquals(TransactionStatus.PENDING,
                    bankingCore.getCashbackStatus(accountA, cashbackTx.transactionId()));
            assertEquals(new BigDecimal("750.00"),
                    bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(25)));

            // Após o vencimento (t + 40s): liquida automaticamente para CONCLUDED e saldo sobe para 755.00
            assertEquals(new BigDecimal("755.00"),
                    bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(40)));
            assertEquals(TransactionStatus.CONCLUDED,
                    bankingCore.getCashbackStatus(accountA, cashbackTx.transactionId()));
        }

        @Test
        @DisplayName("Should reject negative cashback delay")
        void shouldRejectNegativeSeconds() {
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.dispatch(new CashbackPaymentCommand(accountA, new BigDecimal("100.00"), baseTime.plusSeconds(5), -1)));
        }

        @Test
        @DisplayName("Should reject cashback status lookup for unknown transaction or mismatched account")
        void shouldRejectInvalidCashbackStatusLookup() {
            bankingCore.dispatch(new CreateAccountCommand(accountB, baseTime));

            List<Transaction> result = bankingCore.dispatch(
                    new CashbackPaymentCommand(accountA, new BigDecimal("100.00"), baseTime.plusSeconds(5), 10)
            );
            UUID cashbackId = result.get(1).transactionId();

            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.getCashbackStatus(accountA, UUID.randomUUID()));
            assertThrows(IllegalArgumentException.class,
                    () -> bankingCore.getCashbackStatus(accountB, cashbackId));
        }
    }

    @Nested
    @DisplayName("Queries: Balance Point-in-Time, Statement & Filters")
    class QueryTests {

        @BeforeEach
        void populateHistory() {
            bankingCore.dispatch(new CreateAccountCommand(accountA, baseTime));
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("500.00"), baseTime.plusSeconds(10)));
            bankingCore.dispatch(new WithdrawCommand(accountA, new BigDecimal("100.00"), baseTime.plusSeconds(20)));
            bankingCore.dispatch(new DepositCommand(accountA, new BigDecimal("50.00"), baseTime.plusSeconds(30)));
        }

        @Test
        @DisplayName("Should return exact historical balance at any point in time")
        void shouldGetBalanceAtPointInTime() {
            assertEquals(BigDecimal.ZERO, bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(5)));
            assertEquals(new BigDecimal("500.00"), bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(15)));
            assertEquals(new BigDecimal("400.00"), bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(20)));
            assertEquals(new BigDecimal("450.00"), bankingCore.getBalanceAt(accountA, baseTime.plusSeconds(35)));
        }

        @Test
        @DisplayName("Should filter statement by transaction type and time window")
        void shouldFilterTransactions() {
            List<Transaction> deposits = bankingCore.findTransactions(
                    accountA, TransactionType.USER_DEPOSIT, baseTime.plusSeconds(5), baseTime.plusSeconds(35)
            );
            assertEquals(2, deposits.size());

            List<Transaction> windowOnly = bankingCore.findTransactions(
                    accountA, null, baseTime.plusSeconds(15), baseTime.plusSeconds(25)
            );
            assertEquals(1, windowOnly.size());
            assertEquals(TransactionType.USER_WITHDRAW, windowOnly.getFirst().transactionType());
        }
    }
}

