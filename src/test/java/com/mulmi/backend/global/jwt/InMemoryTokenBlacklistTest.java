package com.mulmi.backend.global.jwt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenBlacklistTest {

    private final InMemoryTokenBlacklist tokenBlacklist = new InMemoryTokenBlacklist();

    @Test
    void revokedTokenIsBlockedUntilExpiration() {
        tokenBlacklist.revoke("access-token", Instant.now().plusSeconds(60));

        assertThat(tokenBlacklist.isRevoked("access-token")).isTrue();
    }

    @Test
    void expiredTokenIsRemovedFromBlacklist() {
        tokenBlacklist.revoke("expired-token", Instant.now().minusSeconds(1));

        assertThat(tokenBlacklist.isRevoked("expired-token")).isFalse();
    }
}
