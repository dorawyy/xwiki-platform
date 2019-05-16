/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.integration.junit5;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.ConsoleAppender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Unit tests for {@link ValidateConsoleExtension}.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidateConsoleExtensionTest
{
    @ExtendWith(ValidateConsoleExtension.class)
    public static class SampleTestCase
    {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleTestCase.class);

        @BeforeAll
        static void setUp()
        {
            // Error that goes through (not expected or excluded)
            LOGGER.error("in beforeAll");

            // Warn that is excluded
            LOGGER.warn("caught in beforeAll");
        }

        @AfterAll
        static void validate(LogCaptureConfiguration configuration)
        {
            configuration.registerExcludes("caught in beforeAll");
            configuration.registerExpected("expected");

            // Exclude that didn't happen
            configuration.registerExcludes("exclude that didn't happen");

            // Expected that didn't happen
            configuration.registerExpected("expected that didn't happen");
        }

        @Test
        void doSomething()
        {
            // "Deprecated usage of" that goes through (not expected or excluded)
            LOGGER.error("Deprecated usage of something");

            // Error that is expected
            LOGGER.error("expected");
        }
    }

    @Test
    void verifyConsoleExtension() throws Exception
    {
        PrintStream savedOut = System.out;
        PrintStream savedErr = System.err;
        try {
            ByteArrayOutputStream outContentStream = new ByteArrayOutputStream();
            PrintStream psOut = new PrintStream(outContentStream);
            System.setOut(psOut);

            ByteArrayOutputStream errContentStream = new ByteArrayOutputStream();
            PrintStream psErr = new PrintStream(errContentStream);
            System.setErr(psErr);

            configureLogback();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(SampleTestCase.class))
                .build();
            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
            launcher.execute(request, summaryListener);

            TestExecutionSummary summary = summaryListener.getSummary();
            assertResult(summary, errContentStream.toString());
        } finally {
            resetLogback();
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
    }

    private void assertResult(TestExecutionSummary summary, String errContent)
    {
        assertEquals(1, summary.getFailures().size());
        assertEquals(""
            + "The following lines were matching forbidden content:[\n"
            + "ERROR - in beforeAll\n"
            + "ERROR - Deprecated usage of something\n"
            + "]", summary.getFailures().get(0).getException().getMessage());
        assertEquals(""
            + "WARN  - The following lines were matching excluded patterns and need to be fixed: [\n"
            + "WARN  - caught in beforeAll\n"
            + "]\n"
            + "WARN  - The following excludes were not matched and could be candidates for removal (beware of configs):"
                + " [\n"
            + "exclude that didn't happen\n"
            + "]\n"
            + "WARN  - The following expected were not matched and could be candidates for removal (beware of configs):"
                + " [\n"
            + "expected that didn't happen\n"
            + "]\n", errContent);
    }

    /**
     * Configure the appender to not display dates or thread to make it easy to assert.
     */
    private void configureLogback()
    {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        // Note: the "%-5p" is important since it prints the severity (ERROR, WARN, etc) which is used by the validator.
        ple.setPattern("%-5p - %m%n");
        ple.setContext(context);
        ple.start();
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setTarget("System.out");
        consoleAppender.setEncoder(ple);
        consoleAppender.setContext(context);
        consoleAppender.start();
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.INFO);
    }

    private void resetLogback() throws Exception
    {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        ContextInitializer initializer = new ContextInitializer(context);
        initializer.autoConfig();
    }
}
