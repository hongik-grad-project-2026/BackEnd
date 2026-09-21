package com.mulmi.backend.global.jwt;

import java.time.Instant;

public interface TokenBlacklist {

    void revoke(String token, Instant expiresAt);

    boolean isRevoked(String token);
}
