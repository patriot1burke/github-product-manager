package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.RepositoryConfig;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Unignore a label")
public class UnignoreLabelCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    RepositoryConfigService index;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "0", description = "Regex pattern to ignore a label")
    private String pattern;

    @Override
    public Integer call() throws Exception {
        if (!index.exists(repo.trim())) {
            output.error("Repository [" + repo + "] not found");
            return CommandLine.ExitCode.SOFTWARE;
        }
        RepositoryConfig repoIndex = index.load(repo.trim());
        if (!repoIndex.ignoredLabels.contains(pattern)) {
            output.warn("Label pattern [" + pattern + "] not found in ignore list for " + repo);
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredLabels.remove(pattern);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
