package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.ReportService;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "labels", description = "Report on labels for a given Github repo")
public class ReportLabelsCommand extends BaseCommand implements Runnable {
    @Inject
    ReportService reportService;

    @Inject
    RepositoryConfigService index;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(description = "List of labels to report on.", arity = "1..*")
    private List<String> labels;

    @Override
    public void run() {
        try {
            output.info(reportService.summarizeLabled(repo, labels));
        } catch (Exception e) {
            output.error("Error summarizing labeled issues", e);
        }

    }
}
