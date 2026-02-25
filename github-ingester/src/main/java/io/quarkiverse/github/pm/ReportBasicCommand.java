package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.ReportService;
import io.quarkiverse.github.index.ReportService.BasicReport;
import io.quarkiverse.github.index.ReportService.DateRange;
import io.quarkiverse.github.index.ReportService.LabelReport;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "basic", description = "Basic report on Github data")
public class ReportBasicCommand extends BaseCommand implements Runnable {
    @Inject
    ReportService reportService;

    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Option(names = "--month", required = false, description = "Report on the last 30 days")
    private boolean month = false;

    @Option(names = "--quarter", required = false, description = "Report on the 90 days")
    private boolean quarter = false;

    @Option(names = "--year", required = false, description = "Report on the last year")
    private boolean year = false;

    @Override
    public void run() {
        try {
            RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
            DateRange dateRange = DateRange.LAST_30_DAYS;
            if (quarter) {
                dateRange = DateRange.LAST_90_DAYS;
            } else if (year) {
                dateRange = DateRange.YEAR;
            }
            BasicReport report = reportService.basicReport(repoIndex, dateRange);
            output.info("Label counts");
            for (LabelReport labelReport : report.labelCounts()) {
                output.info("  " + labelReport.name() + ": " + labelReport.count());
            }
            output.info("--------------------------------");
            output.info("Total discussions: " + report.discussions().total());
            output.info("Unlabeled discussions: " + report.discussions().unlabeled());
            output.info("Label discussion counts");
            for (LabelReport labelReport : report.discussions().labelCounts()) {
                output.info("  " + labelReport.name() + ": " + labelReport.count());
            }
            output.info("--------------------------------");
            output.info("Total issues: " + report.issues().total());
            output.info("Unlabeled issues: " + report.issues().unlabeled());
            output.info("Label issue counts");
            for (LabelReport labelReport : report.issues().labelCounts()) {
                output.info("  " + labelReport.name() + ": " + labelReport.count());
            }
        } catch (Exception e) {
            e.printStackTrace();
            output.error("Error pulling basic report", e);
        }

    }
}
