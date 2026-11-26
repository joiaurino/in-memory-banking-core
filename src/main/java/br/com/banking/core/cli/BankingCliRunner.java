package br.com.banking.core.cli;

import br.com.banking.core.domain.AuthSession;
import br.com.banking.core.domain.ClientData;
import br.com.banking.core.domain.Transaction;
import br.com.banking.core.domain.TransactionCommand.*;
import br.com.banking.core.enums.TransactionStatus;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.service.IAuthClient;
import br.com.banking.core.service.IBankingCore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankingCliRunner {

    private final IBankingCore core;
    private final IAuthClient authClient;
    private final ConsoleIO io;
    private final MessageResolver messages;
    private final Map<String, Runnable> publicActions;
    private final Map<String, Runnable> authenticatedActions;

    private AuthSession currentSession;
    private boolean running = true;

    public BankingCliRunner(IBankingCore core, IAuthClient authClient, ConsoleIO io, MessageResolver messages) {
        this.core = core;
        this.authClient = authClient;
        this.io = io;
        this.messages = messages;

        this.publicActions = Map.of(
                "1", this::register,
                "2", this::login,
                "0", this::exit
        );

        this.authenticatedActions = Map.ofEntries(
                Map.entry("1", this::deposit),
                Map.entry("2", this::withdraw),
                Map.entry("3", this::transfer),
                Map.entry("4", this::payWithCashback),
                Map.entry("5", this::processCashbacks),
                Map.entry("6", this::checkCashbackStatus),
                Map.entry("7", this::checkBalance),
                Map.entry("8", this::viewStatement),
                Map.entry("9", this::filterStatement),
                Map.entry("10", this::logout),
                Map.entry("0", this::exit)
        );
    }

    public void start() {
        io.println("app.banner");
        while (running) {
            printMenu();
            String option = io.readLine();
            Map<String, Runnable> activeMenu = isAuthenticated() ? authenticatedActions : publicActions;
            Runnable action = activeMenu.get(option);

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

    private boolean isAuthenticated() {
        return currentSession != null;
    }

    private UUID authenticatedAccountId() {
        if (currentSession == null || !authClient.isTokenValid(currentSession.accountId(), currentSession.token())) {
            currentSession = null;
            throw new IllegalArgumentException(messages.get("app.error.session_expired"));
        }
        return currentSession.accountId();
    }

    private void printMenu() {
        if (!isAuthenticated()) {
            io.println("menu.auth.header");
            io.println("menu.auth.opt.1");
            io.println("menu.auth.opt.2");
            io.println("menu.opt.0");
            io.printRaw(messages.get("menu.prompt"));
            return;
        }

        io.println("menu.header");
        for (int i = 1; i <= 10; i++) {
            io.println("menu.opt." + i);
        }
        io.println("menu.opt.0");
        io.printRaw(messages.get("menu.prompt"));
    }

    private void register() {
        String name = io.readString("prompt.name");
        String lastName = io.readString("prompt.last_name");
        String cpf = io.readString("prompt.cpf");
        String password = io.readString("prompt.password");

        UUID accountId = UUID.randomUUID();
        ClientData clientData = new ClientData(name, lastName, cpf, accountId, "pending");

        this.currentSession = authClient.register(clientData, password);
        io.println("msg.account_created", currentSession.accountId());
    }

    private void login() {
        String cpf = io.readString("prompt.cpf");
        String password = io.readString("prompt.password");

        this.currentSession = authClient.login(cpf, password);
        io.println("msg.login_ok", currentSession.accountId());
    }

    private void logout() {
        if (currentSession != null) {
            authClient.logout(currentSession.token());
            currentSession = null;
        }
        io.println("msg.logout_ok");
    }

    private void deposit() {
        UUID accountId = authenticatedAccountId();
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        Transaction tx = core.dispatch(new DepositCommand(accountId, amount, LocalDateTime.now())).getFirst();
        io.println("msg.deposit_ok", tx.balance());
    }

    private void withdraw() {
        UUID accountId = authenticatedAccountId();
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        Transaction tx = core.dispatch(new WithdrawCommand(accountId, amount, LocalDateTime.now())).getFirst();
        io.println("msg.withdraw_ok", tx.balance());
    }

    private void transfer() {
        UUID sourceId = authenticatedAccountId();
        UUID targetId = io.readUuid("prompt.target_account");
        BigDecimal amount = io.readBigDecimal("prompt.amount");
        List<Transaction> txs = core.dispatch(new TransferCommand(sourceId, targetId, amount, LocalDateTime.now()));
        io.println("msg.transfer_ok", txs.getFirst().balance(), txs.getLast().balance());
    }

    private void payWithCashback() {
        UUID accountId = authenticatedAccountId();
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
        authenticatedAccountId();
        LocalDateTime now = LocalDateTime.now();
        core.processCashback(now);
        io.println("msg.cashback_processed",
                now.format(ConsoleIO.FORMATTER));
    }

    private void checkCashbackStatus() {
        UUID accountId = authenticatedAccountId();
        UUID txId = io.readUuid("prompt.cashback_tx_id");
        TransactionStatus status = core.getCashbackStatus(accountId, txId);
        io.println("msg.cashback_status", status);
    }

    private void checkBalance() {
        UUID accountId = authenticatedAccountId();
        BigDecimal balance = core.getBalanceAt(accountId, LocalDateTime.now());
        io.println("msg.current_balance", balance);
    }

    private void viewStatement() {
        UUID accountId = authenticatedAccountId();
        io.printTransactions(core.getStatement(accountId));
    }

    private void filterStatement() {
        UUID accountId = authenticatedAccountId();
        TransactionType type = io.readTransactionType("prompt.tx_type");
        io.printTransactions(core.findTransactions(accountId, type, null, null));
    }

    private void exit() {
        running = false;
        io.println("app.shutdown");
    }
}