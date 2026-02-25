package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "report", description = "Various reports on Github data.", subcommands = {
        ReportBasicCommand.class })
public class ReportCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        output.info("Subcommands to generate various reports on Github data.");
        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"ingester report <command> --help\" for more information about a given command.");

        return CommandLine.ExitCode.USAGE;
    }

}
