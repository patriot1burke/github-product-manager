package io.quarkiverse.github.pm;

import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;

@Command(name = "show", description = "Show various info about Github projects and ingester.", subcommands = {
        ShowCategoriesCommand.class, ShowLabelsCommand.class, ShowIndexCommand.class })
public class ShowCommand extends BaseCommand implements Runnable {

    @Override
    public void run() {
        output.info("Subcommands to show various info about Github projects and ingester.");
        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"ingester show <command> --help\" for more information about a given command.");

    }

}
