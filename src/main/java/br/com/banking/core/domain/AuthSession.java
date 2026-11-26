package br.com.banking.core.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthSession(
        String token,
        UUID accountId,
        LocalDateTime expiresAt
) {
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
