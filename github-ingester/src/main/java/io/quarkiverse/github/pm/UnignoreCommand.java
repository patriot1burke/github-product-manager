package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "unignore", description = "Unignore things when ingesting.", subcommands = {
        UnignoreCategoryCommand.class, UnignoreLabelCommand.class })
public class UnignoreCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        output.info("Subcommands to unignore things when ingesting.");
        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"ingester unignore <command> --help\" for more information about a given command.");

        return CommandLine.ExitCode.USAGE;
    }

}
