package io.quarkiverse.github.pm;

import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.AppLoggerFactory;
import io.quarkiverse.github.pm.util.BaseCommand;
import io.quarkiverse.github.pm.util.OutputMixin;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "issue-ingester", mixinStandardHelpOptions = true, subcommands = {
        PullCommand.class }, description = "Tool for pulling Github issues, summarizing, and ingesting into a vector database")
public class GithubIssueIngester extends BaseCommand implements Runnable {
    public GithubIssueIngester() {
        AppLogger.Factory.instance = new AppLoggerFactory() {
            @Override
            public AppLogger logger(Class clz) {
                return getOutput();
            }
        };
    }

    public OutputMixin getOutput() {
        return output;
    }

    @Override
    public void run() {
        System.out.println("Hello, World!");
    }

}
