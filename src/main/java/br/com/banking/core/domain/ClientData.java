package br.com.banking.core.domain;

import java.util.UUID;

public record ClientData(
        String name,
        String lastName,
        String cpf,
        UUID accountId,
        String credentials
) {
    public ClientData {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId must not be null");
        }
        if (name == null || name.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Name and lastName are required");
        }
        if (cpf == null || cpf.replaceAll("\\D", "").length() != 11) {
            throw new IllegalArgumentException("CPF must have 11 digits");
        }
        if (credentials == null || credentials.isBlank()) {
            throw new IllegalArgumentException("credentials must not be null");
        }

        cpf = cpf.replaceAll("\\D", "");
    }

    @Override
    public String toString() {
        return "ClientData[accountId=%s, name=%s %s, cpf=***.***.***-%s]"
                .formatted(accountId, name, lastName, cpf.substring(9));
    }
}