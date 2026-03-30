/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import java.util.concurrent.Callable;

import cloud.katta.cli.Katta;
import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(name = "completion",
        description = "Generate shell completion scripts.",
        mixinStandardHelpOptions = true
)
public class Completion implements Callable<Void> {

    private enum Shell {
        BASH
    }

    @CommandLine.Option(names = {"--shell"}, description = "Shell to generate completion for. Defaults to bash.", defaultValue = "bash")
    private Shell shell;

    @Override
    public Void call() {
        var commandLine = new CommandLine(new Katta())
                .setPosixClusteredShortOptionsAllowed(false);
        System.out.print(AutoComplete.bash(commandLine.getCommandName(), commandLine));
        return null;
    }
}
