package com.example.ai_doc.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets a visitor process a few documents before being asked for an account.
 *
 * <p>Demanding a sign-in before anything can be tried is a poor trade for a public demo, and it
 * was also a deadlock here: signing in posts to this service, so a sleeping instance made the
 * sign-in fail, and the sign-in was the only request that would have woken it.
 *
 * <p><strong>This is a speed bump, not a security control.</strong> The key is the caller's IP as
 * reported by the proxy, which the caller can influence, and the counts live in memory and are
 * gone when the instance restarts - which on a free plan is whenever it has been idle a quarter
 * of an hour. Anyone who wants more free runs can have them. What it stops is the accidental
 * case: a page left open re-running, or a crawler walking the endpoint. The account requirement
 * behind it, and {@code app.auth.allowed-emails}, are what actually bound the spend.
 */
@Component
public class FreeAttemptAllowance {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeAttemptAllowance.class);

    /**
     * Ceiling on how many callers are remembered. Without one this map is a slow memory leak that
     * only shows up under the traffic you most wanted to survive. The oldest entry is evicted,
     * which means a caller can regain attempts once enough others have been seen - acceptable for
     * something already documented as approximate.
     */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int freeAttempts;
    private final Map<String, Integer> usedByClient;

    public FreeAttemptAllowance(@Value("${app.auth.free-attempts:3}") int freeAttempts) {
        if (freeAttempts < 0) {
            throw new IllegalArgumentException("app.auth.free-attempts must not be negative");
        }
        this.freeAttempts = freeAttempts;
        this.usedByClient = Collections.synchronizedMap(
                new LinkedHashMap<>(256, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                        return size() > MAX_TRACKED_CLIENTS;
                    }
                });

        LOGGER.info("Unauthenticated callers may process {} document(s) before signing in", freeAttempts);
    }

    public int freeAttempts() {
        return freeAttempts;
    }

    /** How many a caller has left, without spending one. Used by the health endpoint. */
    public int remainingFor(HttpServletRequest request) {
        return Math.max(0, freeAttempts - usedByClient.getOrDefault(clientKey(request), 0));
    }

    /**
     * Spends an attempt if one is available.
     *
     * <p>Spent when the request is authorized rather than when it succeeds. A document that fails
     * therefore costs an attempt, which is the unkind reading - but the alternative is holding a
     * reservation across the whole pipeline and releasing it on every exit path, and getting that
     * wrong grants unlimited free runs rather than one too few. The default is set high enough
     * that a single failure does not end the trial.
     *
     * @return true when the caller may proceed without signing in.
     */
    public boolean tryConsume(HttpServletRequest request) {
        if (freeAttempts == 0) {
            return false;
        }

        String client = clientKey(request);
        synchronized (usedByClient) {
            int used = usedByClient.getOrDefault(client, 0);
            if (used >= freeAttempts) {
                LOGGER.debug("Free attempts exhausted for this caller; a sign-in is now required");
                return false;
            }
            usedByClient.put(client, used + 1);
            LOGGER.info("Free attempt {} of {} used", used + 1, freeAttempts);
            return true;
        }
    }

    /**
     * The caller, as well as it can be known behind a proxy.
     *
     * <p>Render terminates TLS and forwards, so the socket address is the proxy's and every
     * visitor would share one allowance. The first hop of {@code X-Forwarded-For} is the original
     * client - which the client can also set itself, hence the caveat on this class.
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma < 0 ? forwarded : forwarded.substring(0, comma)).strip();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
