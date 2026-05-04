package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.ai.github.scanner.PullCacheService;
import io.quarkiverse.ai.github.scanner.model.TimePeriod;
import io.quarkiverse.ai.github.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pull", description = "Pull issues and discussions from Github")
public class PullCommand extends BaseCommand implements Runnable {
    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Option(names = "--since", required = false, description = "Pull discussions and issues since month, quarter, or year")
    private TimePeriod since = null;

    @Inject
    PullCacheService pullService;

    static AppLogger log = AppLogger.getLogger(PullCommand.class);

    @Override
    public void run() {
        try {
            pullService.pull(repo, since);
        } catch (Exception e) {
            log.error("Error pulling discussions", e);
        }

    }

}
