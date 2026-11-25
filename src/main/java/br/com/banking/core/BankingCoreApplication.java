package br.com.banking.core;

import br.com.banking.core.cli.ConsoleIO;
import br.com.banking.core.cli.MessageResolver;
import br.com.banking.core.cli.BankingCliRunner;
import br.com.banking.core.service.BankingCoreImpl;

public class BankingCoreApplication {
    static void main(String[] args) {
        MessageResolver messages = new MessageResolver("messages");
        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(new BankingCoreImpl(), io, messages).start();
        }
    }
}
