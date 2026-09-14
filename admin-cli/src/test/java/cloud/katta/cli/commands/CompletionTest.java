/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import cloud.katta.cli.Katta;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletionTest {

    @Test
    public void testDefaultShell() {
        assertBashCompletion("completion");
    }

    @Test
    public void testShellArgFormat() {
        // Invocation used by Homebrew with shell_parameter_format: :arg
        assertBashCompletion("completion", "--shell=bash");
    }

    @Test
    public void testShellSeparateValue() {
        // Invocation used by .github/workflows/cli.yml
        assertBashCompletion("completion", "--shell", "bash");
    }

    @Test
    public void testUnsupportedShell() {
        final StringWriter err = new StringWriter();
        final CommandLine commandLine = Katta.commandLine();
        commandLine.setErr(new PrintWriter(err));
        assertEquals(CommandLine.ExitCode.USAGE, commandLine.execute("completion", "--shell=zsh"));
        assertTrue(err.toString().contains("Invalid value for option '--shell'"));
    }

    private static void assertBashCompletion(final String... args) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final StringWriter err = new StringWriter();
        final PrintStream stdout = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            final CommandLine commandLine = Katta.commandLine();
            commandLine.setErr(new PrintWriter(err));
            assertEquals(CommandLine.ExitCode.OK, commandLine.execute(args), err.toString());
        }
        finally {
            System.setOut(stdout);
        }
        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("#!/usr/bin/env bash"));
    }
}
