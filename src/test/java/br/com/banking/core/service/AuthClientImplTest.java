package br.com.banking.core.service;

import br.com.banking.core.domain.AuthSession;
import br.com.banking.core.domain.ClientData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthClientImplTest {

    private IBankingCore bankingCore;
    private IAuthClient authClient;

    @BeforeEach
    void setUp() {
        bankingCore = new BankingCoreImpl();
        authClient = new AuthClientImpl(bankingCore);
    }

    private ClientData buildClient(String cpf, UUID accountId) {
        return new ClientData(
                "Joice",
                "Lima",
                cpf,
                accountId,
                "placeholder"
        );
    }

    @Test
    @DisplayName("Should register client, provision bank account in ledger, and return valid session")
    void shouldRegisterClientAndProvisionAccount() {
        UUID accountId = UUID.randomUUID();
        ClientData client = buildClient("123.456.789-01", accountId);

        AuthSession session = authClient.register(client, "secret123");

        assertNotNull(session);
        assertEquals(accountId, session.accountId());
        assertTrue(authClient.isTokenValid(accountId, session.token()));
        assertEquals(BigDecimal.ZERO, bankingCore.getBalanceAt(accountId, LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should reject duplicate CPF registration regardless of formatting")
    void shouldRejectDuplicateCpf() {
        authClient.register(buildClient("123.456.789-01", UUID.randomUUID()), "secret123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authClient.register(buildClient("12345678901", UUID.randomUUID()), "anotherPass123"));

        assertEquals("CPF already registered", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject weak passwords shorter than 8 characters")
    void shouldRejectShortPassword() {
        ClientData client = buildClient("12345678901", UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> authClient.register(client, "1234567"));
    }

    @Test
    @DisplayName("Should authenticate with valid CPF and password and issue distinct session tokens")
    void shouldLoginSuccessfully() {
        UUID accountId = UUID.randomUUID();
        AuthSession registerSession = authClient.register(buildClient("12345678901", accountId), "secret123");

        AuthSession loginSession = authClient.login("123.456.789-01", "secret123");

        assertEquals(accountId, loginSession.accountId());
        assertNotEquals(registerSession.token(), loginSession.token());
        assertTrue(authClient.isTokenValid(accountId, loginSession.token()));
    }

    @Test
    @DisplayName("Should fail login with wrong password or unknown CPF")
    void shouldRejectInvalidCredentials() {
        authClient.register(buildClient("12345678901", UUID.randomUUID()), "secret123");

        assertThrows(IllegalArgumentException.class, () -> authClient.login("12345678901", "wrongPassword"));
        assertThrows(IllegalArgumentException.class, () -> authClient.login("99999999999", "secret123"));
    }

    @Test
    @DisplayName("Should reject token when paired with a different accountId")
    void shouldRejectTokenForMismatchedAccountId() {
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();
        AuthSession sessionA = authClient.register(buildClient("12345678901", accountA), "secret123");

        assertFalse(authClient.isTokenValid(accountB, sessionA.token()));
    }

    @Test
    @DisplayName("Should invalidate session token upon logout")
    void shouldInvalidateTokenAfterLogout() {
        UUID accountId = UUID.randomUUID();
        AuthSession session = authClient.register(buildClient("12345678901", accountId), "secret123");

        assertTrue(authClient.isTokenValid(accountId, session.token()));

        authClient.logout(session.token());

        assertFalse(authClient.isTokenValid(accountId, session.token()));
    }
}