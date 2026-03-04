package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.ReportService;
import io.quarkiverse.github.index.ReportService.BasicReport;
import io.quarkiverse.github.index.ReportService.LabelReport;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "basic", description = "Basic report on Github data")
public class ReportBasicCommand extends BaseCommand implements Runnable {
    @Inject
    ReportService reportService;

    @Inject
    RepositoryConfigService index;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        try {
            BasicReport report = reportService.basicReport(repo);
            output.info("Start date: " + report.startDate());
            output.info("End date: " + report.endDate());
            output.info("Total discussions: " + report.discussions().total());
            output.info("Total issues: " + report.issues().total());
            output.info("Label totals:");
            for (LabelReport labelReport : report.labelCounts()) {
                output.info("  " + labelReport.name() + ": " + labelReport.count());
            }
        } catch (Exception e) {
            e.printStackTrace();
            output.error("Error pulling basic report", e);
        }

    }
}
