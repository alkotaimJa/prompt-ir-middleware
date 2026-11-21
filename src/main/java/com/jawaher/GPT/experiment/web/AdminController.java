package com.jawaher.GPT.experiment.web;

import com.jawaher.GPT.experiment.ExperimentProperties;
import com.jawaher.GPT.experiment.service.CsvExportService;
import com.jawaher.GPT.experiment.service.StatsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

/**
 * Researcher-only endpoints, protected by the {@code X-Admin-Token} header
 * (value = {@code experiment.admin-token} / env {@code ADMIN_TOKEN}).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StatsService stats;
    private final CsvExportService csv;
    private final ExperimentProperties props;

    public AdminController(StatsService stats, CsvExportService csv, ExperimentProperties props) {
        this.stats = stats;
        this.csv = csv;
        this.props = props;
    }

    @GetMapping("/stats")
    public StatsService.StatsView stats(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        authorise(token);
        return stats.compute();
    }

    @GetMapping("/export/votes.csv")
    public ResponseEntity<byte[]> votes(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        authorise(token);
        return file("votes", csv.votesCsv());
    }

    @GetMapping("/export/responses.csv")
    public ResponseEntity<byte[]> responses(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        authorise(token);
        return file("responses", csv.responsesCsv());
    }

    private void authorise(String token) {
        String expected = props.getAdminToken();
        if (expected == null || expected.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin API is disabled (ADMIN_TOKEN not set).");
        }
        if (token == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid admin token.");
        }
    }

    private static ResponseEntity<byte[]> file(String name, String body) {
        // UTF-8 BOM so Excel opens Arabic/Unicode text correctly.
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + data.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(data, 0, out, bom.length, data.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(out);
    }
}
