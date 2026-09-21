package com.mulmi.backend.global.jwt;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    @Override
    public void revoke(String token, Instant expiresAt) {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        revokedTokens.put(token, expiresAt);
    }

    @Override
    public boolean isRevoked(String token) {
        Instant expiresAt = revokedTokens.get(token);

        if (expiresAt == null) {
            return false;
        }

        if (!expiresAt.isAfter(Instant.now())) {
            revokedTokens.remove(token);
            return false;
        }

        return true;
    }
}
