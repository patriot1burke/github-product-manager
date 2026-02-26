package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "labels", description = "Show labels for a given Github repo")
public class ShowLabelsCommand extends BaseCommand implements Runnable {
    @Inject
    Github github;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus", arity = "1")
    private String repo;

    @Override
    public void run() {
        try {
            IterableConnection<Label> labels = github.repository(repo).labels();
            for (Label label : labels) {
                output.info("[" + label.name() + "]: " + label.description());
            }
        } catch (Exception e) {
            output.error("Error pulling labels", e);
        }

    }
}
