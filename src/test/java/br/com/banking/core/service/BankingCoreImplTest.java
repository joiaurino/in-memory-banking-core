package br.com.banking.core.service;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankingCoreImplTest {

    private BankingCoreImpl bankingCore;
    private UUID accountA;
    private UUID accountB;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        bankingCore = new BankingCoreImpl();
        accountA = UUID.randomUUID();
        accountB = UUID.randomUUID();
        baseTime = LocalDateTime.of(2026, 11, 20, 8, 0, 0);
    }

    @Test
    @DisplayName("Should create account with zero balance and reject duplicate accounts")
    void shouldCreateAccountAndRejectDuplicates() {
        Transaction created = bankingCore.createAccount(accountA, baseTime);

        assertEquals(accountA, created.accountId());
        assertEquals(0, BigDecimal.ZERO.compareTo(created.balance()));
        assertEquals(TransactionType.CREATE_ACCOUNT, created.transactionType());

        assertThrows(IllegalArgumentException.class, () ->
                bankingCore.createAccount(accountA, baseTime.plusMinutes(1)));
    }

    @Test
    @DisplayName("Should deposit and withdraw funds updating balance correctly")
    void shouldDepositAndWithdraw() {
        bankingCore.createAccount(accountA, baseTime);

        Transaction deposit = bankingCore.deposit(accountA, new BigDecimal("500.00"), baseTime.plusMinutes(5));
        assertEquals(0, new BigDecimal("500.00").compareTo(deposit.balance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(deposit.amount()));

        Transaction withdraw = bankingCore.withdraw(accountA, new BigDecimal("150.50"), baseTime.plusMinutes(10));
        assertEquals(0, new BigDecimal("349.50").compareTo(withdraw.balance()));
        assertEquals(0, new BigDecimal("-150.50").compareTo(withdraw.amount()));
    }

    @Test
    @DisplayName("Should throw exception when withdrawing more than available balance")
    void shouldFailWithdrawOnInsufficientFunds() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("100.00"), baseTime.plusMinutes(1));

        assertThrows(IllegalArgumentException.class, () ->
                bankingCore.withdraw(accountA, new BigDecimal("100.01"), baseTime.plusMinutes(2)));
    }

    @Test
    @DisplayName("Should transfer funds atomically between two accounts")
    void shouldTransferBetweenAccounts() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.createAccount(accountB, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("300.00"), baseTime.plusMinutes(1));

        Transaction debitTx = bankingCore.transfer(accountA, accountB, new BigDecimal("120.00"), baseTime.plusMinutes(5));

        assertEquals(0, new BigDecimal("180.00").compareTo(debitTx.balance()));
        assertEquals(0, new BigDecimal("180.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(5))));
        assertEquals(0, new BigDecimal("120.00").compareTo(bankingCore.getBalanceAt(accountB, baseTime.plusMinutes(5))));
    }

    @Test
    @DisplayName("Should fail transfer if source has insufficient funds without crediting target")
    void shouldRollbackOrPreventInvalidTransfer() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.createAccount(accountB, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("50.00"), baseTime.plusMinutes(1));

        assertThrows(IllegalArgumentException.class, () ->
                bankingCore.transfer(accountA, accountB, new BigDecimal("100.00"), baseTime.plusMinutes(2)));

        assertEquals(0, BigDecimal.ZERO.compareTo(bankingCore.getBalanceAt(accountB, baseTime.plusMinutes(2))));
    }

    @Test
    @DisplayName("Should debit payment immediately and credit 2% cashback after scheduled duration")
    void shouldProcessCashbackAfterScheduledTime() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("1000.00"), baseTime.plusMinutes(1));

        // Compra de 200.00 -> debita 200.00 (saldo 800.00) e agenda 4.00 de cashback em 3600s
        Transaction cashbackTx = bankingCore.payWithCashback(accountA, new BigDecimal("200.00"), baseTime.plusMinutes(10), 3600);

        assertEquals(0, new BigDecimal("4.00").compareTo(cashbackTx.amount()));
        assertEquals(TransactionStatus.PENDING, bankingCore.getCashbackStatus(accountA, cashbackTx.transactionId()));
        assertEquals(0, new BigDecimal("800.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(10))));

        // Antes de vencer (30 min depois): cashback ainda não deve ter caído
        bankingCore.processCashback(baseTime.plusMinutes(40));
        assertEquals(0, new BigDecimal("800.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(40))));

        // Após vencer (1h10 depois): cashback de 4.00 deve estar no saldo (804.00)
        bankingCore.processCashback(baseTime.plusMinutes(70));
        assertEquals(0, new BigDecimal("804.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(70))));
    }

    @Test
    @DisplayName("Should return accurate point-in-time historical balance")
    void shouldReturnHistoricalBalance() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("100.00"), baseTime.plusMinutes(10));
        bankingCore.deposit(accountA, new BigDecimal("250.00"), baseTime.plusMinutes(20));
        bankingCore.withdraw(accountA, new BigDecimal("50.00"), baseTime.plusMinutes(30));

        assertEquals(0, BigDecimal.ZERO.compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(5))));
        assertEquals(0, new BigDecimal("100.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(15))));
        assertEquals(0, new BigDecimal("350.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(25))));
        assertEquals(0, new BigDecimal("300.00").compareTo(bankingCore.getBalanceAt(accountA, baseTime.plusMinutes(40))));
    }

    @Test
    @DisplayName("Should return statement in reverse chronological order (newest first)")
    void shouldReturnStatementNewestFirst() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("100.00"), baseTime.plusMinutes(1));
        bankingCore.withdraw(accountA, new BigDecimal("40.00"), baseTime.plusMinutes(2));

        List<Transaction> statement = bankingCore.getStatement(accountA);

        assertEquals(3, statement.size());
        assertEquals(TransactionType.USER_WITHDRAW, statement.get(0).transactionType());
        assertEquals(TransactionType.USER_DEPOSIT, statement.get(1).transactionType());
        assertEquals(TransactionType.CREATE_ACCOUNT, statement.get(2).transactionType());
    }

    @Test
    @DisplayName("Should filter transactions by type and optional date range")
    void shouldFilterTransactions() {
        bankingCore.createAccount(accountA, baseTime);
        bankingCore.deposit(accountA, new BigDecimal("100.00"), baseTime.plusMinutes(10));
        bankingCore.withdraw(accountA, new BigDecimal("20.00"), baseTime.plusMinutes(20));
        bankingCore.deposit(accountA, new BigDecimal("300.00"), baseTime.plusMinutes(30));

        // Apenas depósitos em todo o período (passando from/to null)
        List<Transaction> allDeposits = bankingCore.findTransactions(accountA, TransactionType.USER_DEPOSIT, null, null);
        assertEquals(2, allDeposits.size());

        // Qualquer tipo entre +15min e +35min
        List<Transaction> windowTx = bankingCore.findTransactions(
                accountA, null, baseTime.plusMinutes(15), baseTime.plusMinutes(35));
        assertEquals(2, windowTx.size());
    }
}

