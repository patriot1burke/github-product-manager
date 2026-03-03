package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.PruneService;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Remove cached data for a given Github repo")
public class CleanCommand extends BaseCommand implements Runnable {
    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;
    @Inject
    PruneService pruneService;

    @Inject
    static AppLogger log = AppLogger.getLogger(PullCommand.class);

    @Override
    public void run() {
        try {
            pruneService.clear(repo);
        } catch (Exception e) {
            log.error("Error pulling discussions", e);
        }

    }

}
