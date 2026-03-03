package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.index.RepositoryConfig;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Ignore a label")
public class IgnoreLabelCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    RepositoryConfigService index;

    @Inject
    Github github;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "0", description = "Regex pattern to ignore a label")
    private String pattern;

    @Override
    public Integer call() throws Exception {
        RepositoryConfig repoIndex = index.createIfNotExists(repo.trim());
        repoIndex.addIgnoredLabel(pattern);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
