package com.akshit.comefort.cli;

import com.akshit.comefort.App;
import com.akshit.comefort.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End CLI Tests")
class AppCliTest {

    private DatabaseManager dbManager;
    private App app;
    private CommandLine cmd;
    private ByteArrayOutputStream outStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        app = new App(dbManager);
        cmd = App.createCommandLine(app);

        outStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("App command is named 'comefort' with 'cmf' alias and 'cf' is removed")
    void testCommandNaming() {
        assertEquals("comefort", cmd.getCommandName());
        assertTrue(List.of(cmd.getCommandSpec().aliases()).contains("cmf"));
        assertFalse(List.of(cmd.getCommandSpec().aliases()).contains("cf"));
    }

    @Test
    @DisplayName("cmf init initializes database and prints welcome instructions")
    void testInitCommand() {
        int exitCode = cmd.execute("init");
        assertEquals(0, exitCode);
        String output = outStream.toString();
        assertTrue(output.contains("ComeFort initialized successfully")
                || output.contains("ComeFort is already initialized"));
    }

    @Test
    @DisplayName("cmf c captures thoughts to inbox")
    void testQuickCaptureCommand() {
        int exitCode = cmd.execute("c", "Implement", "new", "feature");
        assertEquals(0, exitCode);
        String output = outStream.toString();
        assertTrue(output.contains("Captured: Implement new feature"));
    }

    @Test
    @DisplayName("cmf project add and list workflow")
    void testProjectWorkflow() {
        int addCode = cmd.execute("project", "add", "HoneyChain", "--desc", "Honey security");
        assertEquals(0, addCode);

        outStream.reset();
        int listCode = cmd.execute("project", "list");
        assertEquals(0, listCode);
        String output = outStream.toString();
        assertTrue(output.contains("HoneyChain"));
    }

    @Test
    @DisplayName("cmf task add, list, and done workflow")
    void testTaskWorkflow() {
        cmd.execute("project", "add", "DevVault");

        int addCode = cmd.execute("task", "add", "Write", "unit", "tests",
                "-p", "DevVault", "--priority", "high", "-d", "today");
        assertEquals(0, addCode);

        outStream.reset();
        int listCode = cmd.execute("task", "list");
        assertEquals(0, listCode);
        String listOutput = outStream.toString();
        assertTrue(listOutput.contains("Write unit tests"));

        outStream.reset();
        int doneCode = cmd.execute("task", "done", "Write unit tests");
        assertEquals(0, doneCode);
        String doneOutput = outStream.toString();
        assertTrue(doneOutput.contains("Completed: Write unit tests"));
    }

    @Test
    @DisplayName("cmf note add, list, and show workflow")
    void testNoteWorkflow() {
        int addCode = cmd.execute("note", "add", "Architecture Notes", "-c", "Microservices vs Modular Monolith");
        assertEquals(0, addCode);

        outStream.reset();
        int listCode = cmd.execute("note", "list");
        assertEquals(0, listCode);
        assertTrue(outStream.toString().contains("Architecture Notes"));

        outStream.reset();
        int showCode = cmd.execute("note", "show", "Architecture");
        assertEquals(0, showCode);
        assertTrue(outStream.toString().contains("Microservices vs Modular Monolith"));
    }

    @Test
    @DisplayName("cmf search finds cross-entity matches")
    void testSearchCommand() {
        cmd.execute("c", "idea: Quantum computing simulator");
        outStream.reset();

        int searchCode = cmd.execute("search", "Quantum");
        assertEquals(0, searchCode);
        assertTrue(outStream.toString().contains("Quantum computing simulator"));
    }

    @Test
    @DisplayName("cmf status and cmf today report stats cleanly")
    void testStatusAndTodayCommands() {
        int statusExit = cmd.execute("status");
        assertEquals(0, statusExit);

        outStream.reset();
        int todayExit = cmd.execute("today");
        assertEquals(0, todayExit);
    }
}
