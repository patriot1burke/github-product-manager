package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.AppLoggerFactory;
import io.quarkiverse.github.pm.util.BaseCommand;
import io.quarkiverse.github.pm.util.OutputMixin;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "ingester", mixinStandardHelpOptions = true, subcommands = {
        PullCommand.class,
        ShowCommand.class,
        ReportCommand.class }, description = "Tool for pulling Github issues, discussions, summarizing, and ingesting into a vector database")
public class Ingester extends BaseCommand implements Callable<Integer> {
    public Ingester() {
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
    public Integer call() {
        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"ingester <command> --help\" for more information about a given command.");

        return CommandLine.ExitCode.USAGE;
    }

}
