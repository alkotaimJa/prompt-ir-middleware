package com.jawaher.GPT.experiment.web;

import com.jawaher.GPT.experiment.service.ExperimentService;
import com.jawaher.GPT.experiment.web.Dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public API used by the participant-facing evaluation UI.
 *
 * <pre>
 * GET  /api/study                      study configuration (task categories, limits)
 * POST /api/participants               register (consent + optional demographics)
 * GET  /api/participants/{id}          progress
 * POST /api/trials/context-options     optional: clarifying context options for a question
 * POST /api/trials                     run raw vs transformed on all providers (blinded)
 * POST /api/trials/{id}/evaluation     submit votes + reasons + best model
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class ExperimentController {

    private final ExperimentService service;

    public ExperimentController(ExperimentService service) {
        this.service = service;
    }

    @GetMapping("/study")
    public StudyInfo study() {
        return service.studyInfo();
    }

    @PostMapping("/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantView register(@Valid @RequestBody RegisterParticipantRequest req) {
        return service.register(req);
    }

    @GetMapping("/participants/{id}")
    public ParticipantView participant(@PathVariable UUID id) {
        return service.participant(id);
    }

    @PostMapping("/trials/context-options")
    public ContextOptionsView contextOptions(@Valid @RequestBody ContextOptionsRequest req, HttpServletRequest http) {
        return service.contextOptions(req, clientIp(http));
    }

    @PostMapping("/trials")
    public TrialView startTrial(@Valid @RequestBody StartTrialRequest req, HttpServletRequest http) {
        return service.startTrial(req, clientIp(http));
    }

    @PostMapping("/trials/{id}/evaluation")
    public EvaluationResult evaluate(@PathVariable UUID id, @Valid @RequestBody EvaluationRequest req) {
        return service.evaluate(id, req);
    }

    static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
