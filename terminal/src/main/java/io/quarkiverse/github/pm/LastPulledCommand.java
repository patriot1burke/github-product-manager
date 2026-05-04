package io.quarkiverse.github.pm;

import java.time.Instant;

import jakarta.inject.Inject;

import io.quarkiverse.ai.github.db.EmbeddingsRepository;
import io.quarkiverse.ai.github.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "lastpulled", description = "Show date of lasted pulled github item")
public class LastPulledCommand extends BaseCommand implements Runnable {
    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Inject
    EmbeddingsRepository db;

    static AppLogger log = AppLogger.getLogger(LastPulledCommand.class);

    @Override
    public void run() {
        try {
            long lastPulled = db.lastPulled(repo);
            log.info("Last pulled: " + Instant.ofEpochMilli(lastPulled).toString());
        } catch (Exception e) {
            log.error("Error pulling discussions", e);
        }

    }

}
