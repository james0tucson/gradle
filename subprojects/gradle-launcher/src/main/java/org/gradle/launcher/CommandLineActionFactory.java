/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher;

import org.gradle.CommandLineArgumentException;
import org.gradle.StartParameter;
import org.gradle.api.Action;
import org.gradle.api.internal.project.ServiceRegistry;
import org.gradle.configuration.GradleLauncherMetaData;
import org.gradle.gradleplugin.userinterface.swing.standalone.BlockingApplication;
import org.gradle.initialization.CommandLineConverter;
import org.gradle.initialization.CommandLineParser;
import org.gradle.initialization.DefaultCommandLineConverter;
import org.gradle.initialization.ParsedCommandLine;
import org.gradle.logging.LoggingConfiguration;
import org.gradle.logging.LoggingManagerInternal;
import org.gradle.logging.LoggingServiceRegistry;
import org.gradle.logging.internal.OutputEventListener;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * <p>Responsible for converting a set of command-line arguments into a {@link Runnable} action.</p>
 */
public class CommandLineActionFactory {
    private static final String HELP = "h";
    private static final String GUI = "gui";
    private static final String VERSION = "v";
    private static final String FOREGROUND = "foreground";
    private static final String DAEMON = "daemon";
    private static final String STOP = "stop";

    /**
     * <p>Converts the given command-line arguments to a {@code Runnable} action which performs the action requested by the
     * command-line args. Does not have any side-effects. Each action will call the supplied {@link
     * ExecutionListener} once it has completed.</p>
     *
     * <p>Implementation note: attempt to defer as much as possible until action execution time.</p>
     *
     * @param args The command-line arguments.
     * @return The action to execute.
     */
    Action<ExecutionListener> convert(List<String> args) {
        CommandLineParser parser = new CommandLineParser();

        CommandLineConverter<StartParameter> startParameterConverter = createStartParameterConverter();
        startParameterConverter.configure(parser);

        parser.option(HELP, "?", "help").hasDescription("Shows this help message");
        parser.option(VERSION, "version").hasDescription("Print version info.");
        parser.option(GUI).hasDescription("Launches a GUI application");
        parser.option(FOREGROUND).hasDescription("Starts the Gradle daemon in the foreground [experimental].");
        parser.option(DAEMON).hasDescription("Uses the Gradle daemon to execute the build. Starts the daemon if not running [experimental].");
        parser.option(STOP).hasDescription("Stops the Gradle daemon if it is running [experimental].");

        LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
        ServiceRegistry loggingServices = createLoggingServices();

        Action<ExecutionListener> action;
        try {
            ParsedCommandLine commandLine = parser.parse(args);
            CommandLineConverter<LoggingConfiguration> loggingConfigurationConverter = loggingServices.get(CommandLineConverter.class);
            loggingConfiguration = loggingConfigurationConverter.convert(commandLine);
            action = createAction(parser, commandLine, startParameterConverter, loggingServices);
        } catch (CommandLineArgumentException e) {
            action = new CommandLineParseFailureAction(parser, e);
        }

        return new WithLoggingAction(loggingConfiguration, loggingServices, action);
    }

    CommandLineConverter<StartParameter> createStartParameterConverter() {
        return new DefaultCommandLineConverter();
    }

    ServiceRegistry createLoggingServices() {
        return new LoggingServiceRegistry();
    }

    private Action<ExecutionListener> createAction(CommandLineParser parser, final ParsedCommandLine commandLine, CommandLineConverter<StartParameter> startParameterConverter, final ServiceRegistry loggingServices) {
        if (commandLine.hasOption(HELP)) {
            return new ActionAdapter(new ShowUsageAction(parser));
        }
        if (commandLine.hasOption(VERSION)) {
            return new ActionAdapter(new ShowVersionAction());
        }
        if (commandLine.hasOption(GUI)) {
            return new ActionAdapter(new ShowGuiAction());
        }
        if (commandLine.hasOption(FOREGROUND)) {
            return new ActionAdapter(new DaemonMain(loggingServices));
        }
        if (commandLine.hasOption(STOP)) {
            return new StopDaemonAction(new DaemonConnector(), loggingServices.get(OutputEventListener.class));
        }

        StartParameter startParameter = startParameterConverter.convert(commandLine);
        if (commandLine.hasOption(DAEMON) || System.getProperty("org.gradle.daemon", "false").equals("true")) {
            return new DaemonBuildAction(loggingServices.get(OutputEventListener.class), new DaemonConnector(), startParameter, commandLine, new File(System.getProperty("user.dir")));
        }

        return new RunBuildAction(startParameter, loggingServices);
    }

    private static void showUsage(PrintStream out, CommandLineParser parser) {
        out.println();
        out.print("USAGE: ");
        new GradleLauncherMetaData().describeCommand(out, "[option...]", "[task...]");
        out.println();
        out.println();
        parser.printUsage(out);
        out.println();
    }

    private static class CommandLineParseFailureAction implements Action<ExecutionListener> {
        private final Exception e;
        private final CommandLineParser parser;

        public CommandLineParseFailureAction(CommandLineParser parser, Exception e) {
            this.parser = parser;
            this.e = e;
        }

        public void execute(ExecutionListener executionListener) {
            System.err.println();
            System.err.println(e.getMessage());
            showUsage(System.err, parser);
            executionListener.onFailure(e);
        }
    }

    private static class ShowUsageAction implements Runnable {
        private final CommandLineParser parser;

        public ShowUsageAction(CommandLineParser parser) {
            this.parser = parser;
        }

        public void run() {
            showUsage(System.out, parser);
        }
    }

    private static class ShowVersionAction implements Runnable {
        public void run() {
            System.out.println(new GradleVersion().prettyPrint());
        }
    }

    static class ShowGuiAction implements Runnable {
        public void run() {
            BlockingApplication.launchAndBlock();
        }
    }

    static class ActionAdapter implements Action<ExecutionListener> {
        private final Runnable action;

        ActionAdapter(Runnable action) {
            this.action = action;
        }

        public void execute(ExecutionListener executionListener) {
            action.run();
        }
    }

    static class WithLoggingAction implements Action<ExecutionListener> {
        private final LoggingConfiguration loggingConfiguration;
        private final ServiceRegistry loggingServices;
        private final Action<ExecutionListener> action;

        public WithLoggingAction(LoggingConfiguration loggingConfiguration, ServiceRegistry loggingServices, Action<ExecutionListener> action) {
            this.loggingConfiguration = loggingConfiguration;
            this.loggingServices = loggingServices;
            this.action = action;
        }

        public void execute(ExecutionListener executionListener) {
            LoggingManagerInternal loggingManager = loggingServices.getFactory(LoggingManagerInternal.class).create();
            loggingManager.setLevel(loggingConfiguration.getLogLevel());
            loggingManager.colorStdOutAndStdErr(loggingConfiguration.isColorOutput());
            loggingManager.start();
            action.execute(executionListener);
        }
    }
}