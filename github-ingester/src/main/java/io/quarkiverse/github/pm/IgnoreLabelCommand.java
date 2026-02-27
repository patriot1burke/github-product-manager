package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Ignore a label")
public class IgnoreLabelCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    GithubIndexService index;

    @Inject
    Github github;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "Regex pattern to ignore a label")
    private String pattern;

    @Override
    public Integer call() throws Exception {
        RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
        repoIndex.addIgnoredLabel(pattern);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
