package br.com.banking.core.cli;

import br.com.banking.core.service.AuthClientImpl;
import br.com.banking.core.service.BankingCoreImpl;
import br.com.banking.core.service.IAuthClient;
import br.com.banking.core.service.IBankingCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingCliRunnerTest {

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUpStreams() {
        outputCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapture, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("MessageResolver should resolve plain keys and formatted placeholders")
    void shouldResolveMessagesCorrectly() {
        MessageResolver resolver = new MessageResolver("messages");

        assertEquals("Shutting down Banking Core...", resolver.get("app.shutdown"));
        assertEquals("Deposit successful! New Balance: 150.00", resolver.get("msg.deposit_ok", "150.00"));
    }

    @Test
    @DisplayName("CLI flow: register -> deposit -> check balance -> logout -> login -> withdraw -> exit")
    void shouldExecuteFullAuthenticatedCliSession() {
        MessageResolver messages = new MessageResolver("messages");
        IBankingCore core = new BankingCoreImpl();
        IAuthClient authClient = new AuthClientImpl(core);

        String firstSessionInput = String.join("\n",
                "1",
                "Joice",
                "Lima",
                "12345678901",
                "strongPass123",
                "1", "300.00",
                "7",
                "99",
                "10",
                "0"
        ) + "\n";

        System.setIn(new ByteArrayInputStream(firstSessionInput.getBytes(StandardCharsets.UTF_8)));

        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(core, authClient, io, messages).start();
        }

        String firstOutput = outputCapture.toString(StandardCharsets.UTF_8);
        assertTrue(firstOutput.contains("Account created & logged in!"));
        assertTrue(firstOutput.contains("Deposit successful! New Balance: 300.00"));
        assertTrue(firstOutput.contains("Current Balance: 300.00"));
        assertTrue(firstOutput.contains("Invalid option. Try again."));
        assertTrue(firstOutput.contains("Logged out successfully."));
        assertTrue(firstOutput.contains("Shutting down Banking Core..."));

        outputCapture.reset();
        String secondSessionInput = String.join("\n",
                "2",
                "123.456.789-01",
                "strongPass123",
                "2", "100.00",
                "7",
                "0"
        ) + "\n";

        System.setIn(new ByteArrayInputStream(secondSessionInput.getBytes(StandardCharsets.UTF_8)));

        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(core, authClient, io, messages).start();
        }

        String secondOutput = outputCapture.toString(StandardCharsets.UTF_8);
        assertTrue(secondOutput.contains("Login successful!"));
        assertTrue(secondOutput.contains("Withdrawal successful! New Balance: 200.00"));
        assertTrue(secondOutput.contains("Current Balance: 200.00"));
        assertTrue(secondOutput.contains("Shutting down Banking Core..."));
    }
}