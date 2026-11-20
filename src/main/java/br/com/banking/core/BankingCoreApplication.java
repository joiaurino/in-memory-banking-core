package br.com.banking.core;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionType;
import br.com.banking.core.service.BankingCoreImpl;
import br.com.banking.core.service.IBankingCore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BankingCoreApplication {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        IBankingCore bankingCore = new BankingCoreImpl();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== In-Memory Banking Core CLI ===");

        while (running) {
            printMenu();
            String option = scanner.nextLine().trim();

            try {
                switch (option) {
                    case "1" -> handleCreateAccount(bankingCore);
                    case "2" -> handleDeposit(bankingCore, scanner);
                    case "3" -> handleWithdraw(bankingCore, scanner);
                    case "4" -> handleTransfer(bankingCore, scanner);
                    case "5" -> handlePayWithCashback(bankingCore, scanner);
                    case "6" -> handleProcessCashbacks(bankingCore);
                    case "7" -> handleCheckBalance(bankingCore, scanner);
                    case "8" -> handleStatement(bankingCore, scanner);
                    case "9" -> handleFilterTransactions(bankingCore, scanner);
                    case "0" -> {
                        running = false;
                        System.out.println("Shutting down Banking Core...");
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("[BUSINESS ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[INPUT ERROR] Invalid format: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("""
                
                Select an operation:
                1 - Create Account
                2 - Deposit
                3 - Withdraw
                4 - Transfer
                5 - Pay with Cashback (2%)
                6 - Process Pending Cashbacks (Now)
                7 - Check Current Balance
                8 - View Full Statement
                9 - Filter Statement by Transaction Type
                0 - Exit
                >\s""");
    }

    private static void handleCreateAccount(IBankingCore core) {
        UUID accountId = UUID.randomUUID();
        Transaction tx = core.createAccount(accountId, LocalDateTime.now());
        System.out.println("Account created successfully!");
        System.out.println("Account ID: " + tx.accountId());
    }

    private static void handleDeposit(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

        Transaction tx = core.deposit(accountId, amount, LocalDateTime.now());
        System.out.println("Deposit successful! New Balance: " + tx.balance());
    }

    private static void handleWithdraw(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

        Transaction tx = core.withdraw(accountId, amount, LocalDateTime.now());
        System.out.println("Withdrawal successful! New Balance: " + tx.balance());
    }

    private static void handleTransfer(IBankingCore core, Scanner scanner) {
        System.out.print("Source Account ID (UUID): ");
        UUID sourceId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Target Account ID (UUID): ");
        UUID targetId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

        Transaction tx = core.transfer(sourceId, targetId, amount, LocalDateTime.now());
        System.out.println("Transfer completed! Source New Balance: " + tx.balance());
    }

    private static void handlePayWithCashback(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Payment Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Seconds until cashback credit (e.g., 10 for quick test): ");
        long seconds = Long.parseLong(scanner.nextLine().trim());

        Transaction cashbackTx = core.payWithCashback(accountId, amount, LocalDateTime.now(), seconds);
        System.out.println("Payment processed! Scheduled Cashback: " + cashbackTx.amount()
                + " | Cashback Tx ID: " + cashbackTx.transactionId()
                + " | Scheduled for: " + cashbackTx.scheduledDate().format(FORMATTER));
    }

    private static void handleProcessCashbacks(IBankingCore core) {
        core.processCashback(LocalDateTime.now());
        System.out.println("Pending cashbacks due up to " + LocalDateTime.now().format(FORMATTER) + " processed.");
    }

    private static void handleCheckBalance(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());

        BigDecimal balance = core.getBalanceAt(accountId, LocalDateTime.now());
        System.out.println("Current Balance: " + balance);
    }

    private static void handleStatement(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());

        List<Transaction> statement = core.getStatement(accountId);
        printTransactions(statement);
    }

    private static void handleFilterTransactions(IBankingCore core, Scanner scanner) {
        System.out.print("Account ID (UUID): ");
        UUID accountId = UUID.fromString(scanner.nextLine().trim());
        System.out.print("Transaction Type (CREATE_ACCOUNT, USER_DEPOSIT, USER_WITHDRAW, TRANSFER, PAYMENT_TRANSFER, CASHBACK_DEPOSIT): ");
        TransactionType type = TransactionType.valueOf(scanner.nextLine().trim().toUpperCase());

        List<Transaction> filtered = core.findTransactions(accountId, type, null, null);
        printTransactions(filtered);
    }

    private static void printTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        System.out.println("--- Statement (Newest First) ---");
        for (Transaction t : transactions) {
            System.out.printf("[%s] %-18s | Amount: %10s | Balance: %10s | Status: %s%n",
                    t.transactionDate().format(FORMATTER),
                    t.transactionType(),
                    t.amount(),
                    t.balance(),
                    t.transactionStatus());
        }
    }
}
