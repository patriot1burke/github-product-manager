package io.quarkiverse.github.pm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.Labels.Label;
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

    @Parameters(index = "1", description = "Name of the label.  do 'ingester show labels' to get list of labels.")
    private String label;

    @Override
    public Integer call() throws Exception {
        RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
        Iterable<Label> labels = github.repository(repo).labels();
        Map<String, Label> labelMap = new HashMap<>();
        for (Label label : labels) {
            labelMap.put(label.name(), label);
        }
        if (!labelMap.containsKey(label)) {
            output.error("Label [" + label + "] not found in " + repo);
            output.info("Available labels: ");
            for (Label label : labelMap.values()) {
                output.thinking("[" + label.name() + "]: " + label.description());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredLabels.add(label);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
