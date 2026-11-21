package com.jawaher.GPT.experiment.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatsServiceTest {

    @Test
    void signTest_matchesKnownBinomialValues() {
        // 9 of 10: two-sided p = 2 * (C(10,9)+C(10,10)) / 1024 = 22/1024
        assertThat(StatsService.signTest(9, 10)).isCloseTo(22.0 / 1024, within(1e-9));
        // Perfect split is not significant
        assertThat(StatsService.signTest(5, 10)).isEqualTo(1.0);
        // Symmetric
        assertThat(StatsService.signTest(1, 10)).isCloseTo(StatsService.signTest(9, 10), within(1e-12));
        // 60 of 100: p ≈ 0.0569
        assertThat(StatsService.signTest(60, 100)).isCloseTo(0.0569, within(0.0005));
    }

    @Test
    void wilsonInterval_containsPointEstimate() {
        double[] ci = StatsService.wilson(30, 50);
        assertThat(ci[0]).isLessThan(0.6);
        assertThat(ci[1]).isGreaterThan(0.6);
        assertThat(ci[0]).isCloseTo(0.4618, within(0.001));
        assertThat(ci[1]).isCloseTo(0.7239, within(0.001));
    }

    @Test
    void csvEscape_quotesAndNeutralisesFormulas() {
        assertThat(CsvExportService.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvExportService.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvExportService.escape("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(CsvExportService.escape("- list item")).isEqualTo("- list item");
    }
}
