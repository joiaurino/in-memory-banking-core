package br.com.banking.core.service;

import br.com.banking.core.domain.AuthSession;
import br.com.banking.core.domain.ClientData;
import br.com.banking.core.domain.TransactionCommand.CreateAccountCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthClientImpl implements IAuthClient {

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final String CREDENTIAL_SEPARATOR = ":";

    private final IBankingCore bankingCore;
    private final Map<String, ClientData> clientsByCpf;
    private final Map<String, AuthSession> activeSessions;
    private final SecureRandom secureRandom;

    public AuthClientImpl(IBankingCore bankingCore) {
        if (bankingCore == null) {
            throw new IllegalArgumentException("bankingCore must not be null");
        }
        this.bankingCore = bankingCore;
        this.clientsByCpf = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.secureRandom = new SecureRandom();
    }

    @Override
    public AuthSession register(ClientData clientData, String rawPassword) {
        if (clientData == null) {
            throw new IllegalArgumentException("clientData must not be null");
        }
        validateRawPassword(rawPassword);

        if (clientsByCpf.containsKey(clientData.cpf())) {
            throw new IllegalArgumentException("CPF already registered");
        }

        String salt = generateSalt();
        String hash = hashPassword(rawPassword, salt);
        String saltedHash = salt + CREDENTIAL_SEPARATOR + hash;

        ClientData persistedClient = new ClientData(
                clientData.name(),
                clientData.lastName(),
                clientData.cpf(),
                clientData.accountId(),
                saltedHash
        );

        bankingCore.dispatch(new CreateAccountCommand(persistedClient.accountId(), LocalDateTime.now()));
        clientsByCpf.put(persistedClient.cpf(), persistedClient);

        return createSession(persistedClient.accountId());
    }

    @Override
    public AuthSession login(String cpf, String rawPassword) {
        if (cpf == null || rawPassword == null) {
            throw new IllegalArgumentException("CPF and password are required");
        }

        String normalizedCpf = cpf.replaceAll("\\D", "");
        ClientData client = clientsByCpf.get(normalizedCpf);

        if (client == null || !verifyPassword(rawPassword, client.credentials())) {
            throw new IllegalArgumentException("Invalid CPF or password");
        }

        return createSession(client.accountId());
    }

    @Override
    public boolean isTokenValid(UUID accountId, String token) {
        if (accountId == null || token == null || token.isBlank()) {
            return false;
        }

        AuthSession session = activeSessions.get(token);
        if (session == null) {
            return false;
        }

        if (session.isExpired(LocalDateTime.now())) {
            activeSessions.remove(token);
            return false;
        }

        return session.accountId().equals(accountId);
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    private boolean verifyPassword(String rawPassword, String storedSaltedHash) {
        if (storedSaltedHash == null) {
            return false;
        }

        String[] parts = storedSaltedHash.split(CREDENTIAL_SEPARATOR, 2);
        if (parts.length != 2) {
            return false;
        }

        String salt = parts[0];
        String expectedHash = parts[1];
        String actualHash = hashPassword(rawPassword, salt);

        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private AuthSession createSession(UUID accountId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        AuthSession session = new AuthSession(token, accountId, LocalDateTime.now().plus(SESSION_TTL));
        activeSessions.put(token, session);
        return session;
    }

    private void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must have at least 8 characters");
        }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}