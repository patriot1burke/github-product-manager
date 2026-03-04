package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ignore", description = "Ignore things when ingesting.", subcommands = {
        IgnoreCategoryCommand.class, IgnoreLabelCommand.class })
public class IgnoreCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        output.info("Subcommands to ignore things when ingesting.");
        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"ingester ignore <command> --help\" for more information about a given command.");

        return CommandLine.ExitCode.USAGE;
    }

}
