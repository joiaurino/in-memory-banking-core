package br.com.banking.core;

import br.com.banking.core.cli.BankingCliRunner;
import br.com.banking.core.cli.ConsoleIO;
import br.com.banking.core.cli.MessageResolver;
import br.com.banking.core.service.AuthClientImpl;
import br.com.banking.core.service.BankingCoreImpl;
import br.com.banking.core.service.IAuthClient;
import br.com.banking.core.service.IBankingCore;

public class BankingCoreApplication {
    static void main(String[] args) {
        MessageResolver messages = new MessageResolver("messages");
        IBankingCore core = new BankingCoreImpl();
        IAuthClient authClient = new AuthClientImpl(core);

        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(core, authClient, io, messages).start();
        }
    }
}