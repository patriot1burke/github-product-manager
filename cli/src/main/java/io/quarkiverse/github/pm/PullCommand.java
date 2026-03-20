package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.PruneService;
import io.quarkiverse.github.index.PullService;
import io.quarkiverse.github.index.model.Earlier;
import io.quarkiverse.github.pm.util.BaseCommand;
import io.quarkiverse.github.util.AppLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pull", description = "Pull issues and discussionsfrom Github")
public class PullCommand extends BaseCommand implements Runnable {
    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Option(names = "--since", required = false, description = "Pull discussions and issues since month, quarter, or year")
    private Earlier since = null;

    @Option(names = "--prune", required = false, description = "Prune discussions and issues older than the given date range")
    private boolean prune = false;

    @Inject
    PruneService pruneService;

    @Inject
    PullService pullService;

    static AppLogger log = AppLogger.getLogger(PullCommand.class);

    @Override
    public void run() {
        try {
            pullService.pull(repo, since);
            if (prune) {
                pruneService.prune(repo, since);
            }
        } catch (Exception e) {
            log.error("Error pulling discussions", e);
        }

    }

}
