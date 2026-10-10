package com.docreview.review;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.docreview.persistence.entity.DocFindingEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SafetyScoreCalculatorTest {
    private final SafetyScoreCalculator calculator = new SafetyScoreCalculator();

    @Test
    void deductsPointsBySeverityAndStopsAtZero() {
        DocFindingEntity critical = new DocFindingEntity();
        critical.setSeverity("critical");
        DocFindingEntity major = new DocFindingEntity();
        major.setSeverity("major");
        DocFindingEntity minor = new DocFindingEntity();
        minor.setSeverity("minor");

        assertEquals(74, calculator.calculate(List.of(critical, major, minor)));
        assertEquals(0, calculator.calculate(List.of(critical, critical, critical, critical, critical, critical, critical)));
    }
}
