package com.fleettrackpro;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionService {
    private static final long SESSION_HOURS = 8;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public record Session(Integer userId, String idEmpresa, boolean administradorSaas, Instant expiresAt) {
    }

    public String create(Integer userId, String idEmpresa, boolean administradorSaas) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        sessions.put(token, new Session(userId, idEmpresa, administradorSaas,
                Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS)));
        return token;
    }

    public Session validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session;
    }
}
