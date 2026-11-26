package br.com.banking.core.cli;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.*;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.service.IBankingCore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankingCliRunner {

    private final IBankingCore core;
    private final ConsoleIO io;
    private final MessageResolver messages;
    private final Map<String, Runnable> actions;
    private boolean running = true;

    public BankingCliRunner(IBankingCore core, ConsoleIO io, MessageResolver messages) {
        this.core = core;
        this.io = io;
        this.messages = messages;
        this.actions = Map.ofEntries(
                Map.entry("1", this::createAccount),
                Map.entry("2", this::deposit),
                Map.entry("3", this::withdraw),
                Map.entry("4", this::transfer),
                Map.entry("5", this::payWithCashback),
                Map.entry("6", this::processCashbacks),
                Map.entry("7", this::checkCashbackStatus),
                Map.entry("8", this::checkBalance),
                Map.entry("9", this::viewStatement),
                Map.entry("10", this::filterStatement),
                Map.entry("0", this::exit)
        );
    }

    public void start() {
        io.println("app.banner");
        while (running) {
            printMenu();
            String option = io.readLine();
            Runnable action = actions.get(option);

            if (action == null) {
                io.println("app.invalid_option");
                continue;
            }

            try {
                action.run();
            } catch (IllegalArgumentException e) {
                io.println("app.error.business", e.getMessage());
            } catch (Exception e) {
                io.println("app.error.input", e.getMessage());
            }
        }
    }

    private void printMenu() {
        io.println("menu.header");
        for (int i = 1; i <= 10; i++) {
            io.println("menu.opt." + i);
        }
        io.println("menu.opt.0");
        io.printRaw(messages.get("menu.prompt"));
    }

    private void createAccount() {
        UUID accountId = UUID.randomUUID();
        core.dispatch(new CreateAccountCommand(accountId, LocalDateTime.now()));
        io.println("msg.account_created", accountId);
    }

    private void deposit() {
        UUID accountId = io.readUuid("prompt.account");
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        Transaction tx = core.dispatch(new DepositCommand(accountId, amount, LocalDateTime.now())).getFirst();
        io.println("msg.deposit_ok", tx.balance());
    }

    private void withdraw() {
        UUID accountId = io.readUuid("prompt.account");
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        Transaction tx = core.dispatch(new WithdrawCommand(accountId, amount, LocalDateTime.now())).getFirst();
        io.println("msg.withdraw_ok", tx.balance());
    }

    private void transfer() {
        UUID sourceId = io.readUuid("prompt.source_account");
        UUID targetId = io.readUuid("prompt.target_account");
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        List<Transaction> txs = core.dispatch(new TransferCommand(sourceId, targetId, amount, LocalDateTime.now()));
        io.println("msg.transfer_ok", txs.getFirst().balance(), txs.getLast().balance());
    }

    private void payWithCashback() {
        UUID accountId = io.readUuid("prompt.account");
        BigDecimal amount = io.readBigDecimal("prompt.payment_amount");
        long seconds = io.readLong("prompt.cashback_seconds");

        List<Transaction> txs = core.dispatch(new CashbackPaymentCommand(accountId, amount, LocalDateTime.now(), seconds));
        Transaction payment = txs.getFirst();
        Transaction cashback = txs.get(1);

        io.println("msg.cashback_scheduled",
                payment.balance(),
                cashback.amount(),
                cashback.transactionId(),
                cashback.scheduledDate().format(ConsoleIO.FORMATTER));
    }

    private void processCashbacks() {
        LocalDateTime now = LocalDateTime.now();
        core.processCashback(now);
        io.println("msg.cashback_processed", now.format(ConsoleIO.FORMATTER));
    }

    private void checkCashbackStatus() {
        UUID accountId = io.readUuid("prompt.account");
        UUID txId = io.readUuid("prompt.cashback_tx_id");
        TransactionStatus status = core.getCashbackStatus(accountId, txId);
        io.println("msg.cashback_status", status);
    }

    private void checkBalance() {
        UUID accountId = io.readUuid("prompt.account");
        BigDecimal balance = core.getBalanceAt(accountId, LocalDateTime.now());
        io.println("msg.current_balance", balance);
    }

    private void viewStatement() {
        UUID accountId = io.readUuid("prompt.account");
        io.printTransactions(core.getStatement(accountId));
    }

    private void filterStatement() {
        UUID accountId = io.readUuid("prompt.account");
        TransactionType type = io.readTransactionType("prompt.tx_type");
        io.printTransactions(core.findTransactions(accountId, type, null, null));
    }

    private void exit() {
        running = false;
        io.println("app.shutdown");
    }
}
