package com.jawaher.GPT.experiment.service;

import com.jawaher.GPT.experiment.ExperimentProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory sliding-window limiter (trials per IP per hour). Protects the
 * study's API budget when the platform is publicly reachable. Sufficient for a
 * single-instance deployment.
 */
@Component
public class RateLimiter {

    private static final long WINDOW_MS = Duration.ofHours(1).toMillis();

    private final ExperimentProperties props;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(ExperimentProperties props) {
        this.props = props;
    }

    public boolean tryAcquire(String key) {
        int limit = props.getMaxTrialsPerIpPerHour();
        if (limit <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key == null ? "unknown" : key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                q.pollFirst();
            }
            if (q.size() >= limit) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }
}
