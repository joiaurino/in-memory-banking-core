package br.com.banking.core.cli;

import br.com.banking.core.service.BankingCoreImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("CLI flow: create account -> deposit -> check balance -> invalid option -> exit")
    void shouldExecuteFullCliSession() {
        String firstSessionInput = "1\n0\n";
        System.setIn(new ByteArrayInputStream(firstSessionInput.getBytes(StandardCharsets.UTF_8)));

        MessageResolver messages = new MessageResolver("messages");
        BankingCoreImpl core = new BankingCoreImpl();

        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(core, io, messages).start();
        }

        String firstOutput = outputCapture.toString(StandardCharsets.UTF_8);
        Matcher uuidMatcher = Pattern.compile("[0-9a-fA-F\\-]{36}").matcher(firstOutput);
        assertTrue(uuidMatcher.find(), "Should print generated account UUID");
        String accountId = uuidMatcher.group();

        // 2º: Usa a mesma instância do core para depositar, consultar saldo, testar opção inválida e sair
        outputCapture.reset();
        String secondSessionInput = String.join("\n",
                "2", accountId, "300.00",
                "8", accountId,
                "99",
                "0"
        ) + "\n";
        System.setIn(new ByteArrayInputStream(secondSessionInput.getBytes(StandardCharsets.UTF_8)));

        try (ConsoleIO io = new ConsoleIO(messages)) {
            new BankingCliRunner(core, io, messages).start();
        }

        String secondOutput = outputCapture.toString(StandardCharsets.UTF_8);
        assertTrue(secondOutput.contains("Deposit successful! New Balance: 300.00"));
        assertTrue(secondOutput.contains("Current Balance: 300.00"));
        assertTrue(secondOutput.contains("Invalid option. Try again."));
        assertTrue(secondOutput.contains("Shutting down Banking Core..."));
    }
}