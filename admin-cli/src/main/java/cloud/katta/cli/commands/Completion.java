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

    @CommandLine.Option(names = {"--shell"}, description = "Shell to generate completion for. Defaults to bash.", defaultValue = "bash")
    private String shell;

    @Override
    public Void call() {
        if(!"bash".equalsIgnoreCase(shell)) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "Unsupported shell: " + shell + " (only bash is supported)");
        }
        var commandLine = new CommandLine(new Katta())
                .setPosixClusteredShortOptionsAllowed(false);
        System.out.print(AutoComplete.bash(commandLine.getCommandName(), commandLine));
        return null;
    }
}
