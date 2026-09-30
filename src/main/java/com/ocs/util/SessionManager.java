package com.ocs.util;

import com.ocs.Database;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory session store with inactivity timeout (SRS SC-5). */
public final class SessionManager {
    private static final class Session {
        final int userId;
        volatile long lastAccess = System.currentTimeMillis();
        Session(int userId) { this.userId = userId; }
    }

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    private SessionManager() { }

    public static String create(int userId) {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        SESSIONS.put(token, new Session(userId));
        return token;
    }

    /** Returns the user id for a valid, unexpired token (and refreshes it), otherwise null. */
    public static Integer getUserId(String token) {
        if (token == null) return null;
        Session s = SESSIONS.get(token);
        if (s == null) return null;
        long timeout = Database.getInt("session.timeout.minutes", 30) * 60_000L;
        long now = System.currentTimeMillis();
        if (now - s.lastAccess > timeout) {
            SESSIONS.remove(token);
            return null;
        }
        s.lastAccess = now;
        return s.userId;
    }

    public static void destroy(String token) {
        if (token != null) SESSIONS.remove(token);
    }
}
