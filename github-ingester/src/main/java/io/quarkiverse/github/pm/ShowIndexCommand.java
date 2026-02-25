package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "index", description = "Show index settings for a given Github repo")
public class ShowIndexCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        if (index.exists(repo.trim())) {
            RepositoryIndex repoIndex = index.load(repo.trim());
            output.info("Ignored discussion categories: ");
            for (String name : repoIndex.ignoredCategories) {
                output.info("   " + name);
            }
            output.info("Ignored labels: ");
            for (String name : repoIndex.ignoredLabels) {
                output.info("   " + name);
            }
        }
    }
}
