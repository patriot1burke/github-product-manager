package io.quarkiverse.ai.github.scanner.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum TimePeriod {

    month(30),
    quarter(90),
    year(365)

    ;

    int days;

    TimePeriod(int days) {
        this.days = days;
    }

    public Instant fromInstant() {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    public long fromMillis() {
        Instant today = Instant.now();
        return today.minus(days, ChronoUnit.DAYS).toEpochMilli();
    }

    public String fromString() {
        Instant today = Instant.now();
        return today.minus(days, ChronoUnit.DAYS).toString();
    }

}