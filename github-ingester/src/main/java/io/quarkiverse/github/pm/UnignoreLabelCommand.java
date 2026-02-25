package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Unignore a label")
public class UnignoreLabelCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "ID of the label.  do 'ingester show labels' to get the ID if you don't know it.")
    private String label;

    @Override
    public Integer call() throws Exception {
        if (!index.exists(repo.trim())) {
            output.error("Repository [" + repo + "] not found");
            return CommandLine.ExitCode.SOFTWARE;
        }
        RepositoryIndex repoIndex = index.load(repo.trim());
        if (!repoIndex.ignoredLabels.contains(label)) {
            output.warn("Label [" + label + "] not found in ignore list for " + repo);
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredLabels.remove(label);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
