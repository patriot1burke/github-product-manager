package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.RepositoryConfig;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "config", description = "Show configuration settings for a given Github repo")
public class ShowConfigCommand extends BaseCommand implements Runnable {
    @Inject
    RepositoryConfigService index;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        if (index.exists(repo.trim())) {
            RepositoryConfig repoIndex = index.load(repo.trim());
            output.info("Ignored discussion categories: ");
            for (String name : repoIndex.ignoredCategories) {
                output.info("   " + name);
            }
            output.info("Ignored label patterns: ");
            for (String name : repoIndex.ignoredLabels) {
                output.info("   " + name);
            }
        }
    }
}
