package com.jawaher.GPT.experiment.service;

import com.jawaher.GPT.experiment.domain.LlmResponse;
import com.jawaher.GPT.experiment.domain.Preference;
import com.jawaher.GPT.experiment.domain.Trial;
import com.jawaher.GPT.experiment.domain.Vote;
import com.jawaher.GPT.experiment.repository.LlmResponseRepository;
import com.jawaher.GPT.experiment.repository.ParticipantRepository;
import com.jawaher.GPT.experiment.repository.TrialRepository;
import com.jawaher.GPT.experiment.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates the collected votes into the statistics used to answer RQ1
 * ("Do users prefer responses generated from transformed prompts over raw prompts?").
 * <p>
 * For each group the transformed-prompt win rate is computed over decisive votes
 * (ties and "both bad" excluded), with a Wilson 95% confidence interval and an
 * exact two-sided binomial sign test against H0: p = 0.5.
 */
@Service
public class StatsService {

    private final VoteRepository votes;
    private final TrialRepository trials;
    private final ParticipantRepository participants;
    private final LlmResponseRepository responses;

    public StatsService(VoteRepository votes, TrialRepository trials,
                        ParticipantRepository participants, LlmResponseRepository responses) {
        this.votes = votes;
        this.trials = trials;
        this.participants = participants;
        this.responses = responses;
    }

    public record PreferenceStats(long raw, long transformed, long tie, long neither, long total,
                                  long decisive, Double transformedWinRate, Double ciLow, Double ciHigh,
                                  Double signTestP) {
    }

    public record CallStats(String provider, String condition, long calls, long failures,
                            double avgLatencyMs, double avgResponseChars) {
    }

    public record ReasonView(Instant at, String provider, String taskCategory, String preference, String reason) {
    }

    public record StatsView(long participants, long trials, long evaluatedTrials, long votes,
                            PreferenceStats overall,
                            Map<String, PreferenceStats> byProvider,
                            Map<String, PreferenceStats> byTaskCategory,
                            Map<String, Long> bestModelCounts,
                            List<CallStats> calls,
                            List<ReasonView> recentReasons) {
    }

    @Transactional(readOnly = true)
    public StatsView compute() {
        List<Vote> all = votes.findAllWithTrial();

        Map<String, PreferenceStats> byProvider = group(all, Vote::getProvider);
        Map<String, PreferenceStats> byCategory = group(all,
                v -> Optional.ofNullable(v.getTrial().getTaskCategory()).orElse("Other"));

        Map<String, Long> best = trials.findAllWithParticipant().stream()
                .filter(Trial::isEvaluated)
                .collect(Collectors.groupingBy(Trial::getBestProvider, TreeMap::new, Collectors.counting()));

        List<CallStats> calls = responses.findAllWithTrial().stream()
                .collect(Collectors.groupingBy(r -> r.getProvider() + "|" + r.getCondition(), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    List<LlmResponse> rs = e.getValue();
                    String[] k = e.getKey().split("\\|");
                    long failures = rs.stream().filter(r -> !r.isSuccess()).count();
                    double lat = rs.stream().mapToLong(LlmResponse::getLatencyMs).average().orElse(0);
                    double len = rs.stream().filter(LlmResponse::isSuccess)
                            .mapToInt(r -> r.getResponseText() == null ? 0 : r.getResponseText().length())
                            .average().orElse(0);
                    return new CallStats(k[0], k[1], rs.size(), failures, round(lat, 0), round(len, 0));
                })
                .toList();

        List<ReasonView> reasons = all.stream()
                .sorted(Comparator.comparing(Vote::getCreatedAt).reversed())
                .limit(100)
                .map(v -> new ReasonView(v.getCreatedAt(), v.getProvider(), v.getTrial().getTaskCategory(),
                        v.getPreference().name(), v.getReason()))
                .toList();

        return new StatsView(participants.count(), trials.count(), trials.countByEvaluatedAtIsNotNull(),
                all.size(), summarise(all), byProvider, byCategory, best, calls, reasons);
    }

    private static Map<String, PreferenceStats> group(List<Vote> all, Function<Vote, String> key) {
        return all.stream()
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> summarise(e.getValue()),
                        (a, b) -> a, LinkedHashMap::new));
    }

    static PreferenceStats summarise(List<Vote> vs) {
        long raw = count(vs, Preference.RAW);
        long tr = count(vs, Preference.TRANSFORMED);
        long tie = count(vs, Preference.TIE);
        long neither = count(vs, Preference.NEITHER);
        long decisive = raw + tr;
        if (decisive == 0) {
            return new PreferenceStats(raw, tr, tie, neither, vs.size(), 0, null, null, null, null);
        }
        double p = (double) tr / decisive;
        double[] ci = wilson(tr, decisive);
        return new PreferenceStats(raw, tr, tie, neither, vs.size(), decisive,
                round(p, 4), round(ci[0], 4), round(ci[1], 4), round(signTest(tr, decisive), 6));
    }

    private static long count(List<Vote> vs, Preference p) {
        return vs.stream().filter(v -> v.getPreference() == p).count();
    }

    /** Wilson score interval, 95%. */
    static double[] wilson(long successes, long n) {
        double z = 1.959963984540054;
        double phat = (double) successes / n;
        double denom = 1 + z * z / n;
        double centre = (phat + z * z / (2.0 * n)) / denom;
        double half = z * Math.sqrt(phat * (1 - phat) / n + z * z / (4.0 * n * n)) / denom;
        return new double[]{Math.max(0, centre - half), Math.min(1, centre + half)};
    }

    /** Exact two-sided binomial (sign) test of k successes in n trials against p = 0.5. */
    static double signTest(long k, long n) {
        long extreme = Math.max(k, n - k);
        double logHalfN = n * Math.log(0.5);
        double tail = 0;
        for (long i = extreme; i <= n; i++) {
            tail += Math.exp(logChoose(n, i) + logHalfN);
        }
        return Math.min(1.0, 2 * tail);
    }

    private static double logChoose(long n, long k) {
        return logFactorial(n) - logFactorial(k) - logFactorial(n - k);
    }

    private static double logFactorial(long n) {
        double s = 0;
        for (long i = 2; i <= n; i++) {
            s += Math.log(i);
        }
        return s;
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }
}
