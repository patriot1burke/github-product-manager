package io.quarkiverse.github.pm.util;

import picocli.CommandLine;

public class HelpOption {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
    public boolean help;
}
