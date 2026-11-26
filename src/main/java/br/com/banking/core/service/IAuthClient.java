package br.com.banking.core.service;

import br.com.banking.core.domain.AuthSession;
import br.com.banking.core.domain.ClientData;

import java.util.UUID;

public interface IAuthClient {
    AuthSession register(ClientData clientData, String rawPassword);
    AuthSession login(String cpf, String rawPassword);
    boolean isTokenValid(UUID accountId, String token);
    void logout(String token);
}
