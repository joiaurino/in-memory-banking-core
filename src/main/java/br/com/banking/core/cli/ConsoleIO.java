package br.com.banking.core.cli;

import br.com.banking.core.domain.Transaction;
import br.com.banking.core.enums.TransactionType;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleIO implements AutoCloseable {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner;
    private final MessageResolver messages;

    public ConsoleIO(MessageResolver messages) {
        this.scanner = new Scanner(System.in);
        this.messages = messages;
    }

    public String readLine() {
        return scanner.nextLine().trim();
    }

    public UUID readUuid(String promptKey) {
        System.out.print(messages.get(promptKey));
        return UUID.fromString(readLine());
    }

    public BigDecimal readBigDecimal(String promptKey) {
        System.out.print(messages.get(promptKey));
        return new BigDecimal(readLine());
    }

    public long readLong(String promptKey) {
        System.out.print(messages.get(promptKey));
        return Long.parseLong(readLine());
    }

    public TransactionType readTransactionType(String promptKey) {
        System.out.print(messages.get(promptKey));
        return TransactionType.valueOf(readLine().toUpperCase());
    }

    public void println(String messageKey, Object... args) {
        System.out.println(messages.get(messageKey, args));
    }

    public void printRaw(String text) {
        System.out.print(text);
    }

    public void printTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            println("msg.statement_empty");
            return;
        }
        println("msg.statement_header");
        for (Transaction t : transactions) {
            System.out.printf("[%s] %-18s | Amount: %10s | Balance: %10s | Status: %s%n",
                    t.transactionDate().format(FORMATTER),
                    t.transactionType(),
                    t.amount(),
                    t.balance(),
                    t.transactionStatus());
        }
    }

    @Override
    public void close() {
        scanner.close();
    }
}