package com.akshit.comefort.util;

import com.akshit.comefort.core.enums.CaptureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InputParser Tests")
class InputParserTest {

    @Test
    @DisplayName("parse detects task: prefix")
    void testParseTaskPrefix() {
        InputParser.ParsedInput result = InputParser.parse("task: Implement database layer");
        assertEquals(CaptureType.TASK, result.type());
        assertEquals("Implement database layer", result.content());
    }

    @Test
    @DisplayName("parse detects note: prefix")
    void testParseNotePrefix() {
        InputParser.ParsedInput result = InputParser.parse("note: Docker compose setup tips");
        assertEquals(CaptureType.NOTE, result.type());
        assertEquals("Docker compose setup tips", result.content());
    }

    @Test
    @DisplayName("parse detects idea: prefix")
    void testParseIdeaPrefix() {
        InputParser.ParsedInput result = InputParser.parse("idea: AI smart assistant for terminal");
        assertEquals(CaptureType.IDEA, result.type());
        assertEquals("AI smart assistant for terminal", result.content());
    }

    @Test
    @DisplayName("parse defaults to AUTO for un-prefixed inputs")
    void testParseAutoDefault() {
        InputParser.ParsedInput result = InputParser.parse("Need to buy more coffee");
        assertEquals(CaptureType.AUTO, result.type());
        assertEquals("Need to buy more coffee", result.content());
    }

    @Test
    @DisplayName("parse handles empty and null inputs safely")
    void testParseEmpty() {
        InputParser.ParsedInput nullResult = InputParser.parse(null);
        assertEquals(CaptureType.AUTO, nullResult.type());
        assertEquals("", nullResult.content());

        InputParser.ParsedInput emptyResult = InputParser.parse("   ");
        assertEquals(CaptureType.AUTO, emptyResult.type());
        assertEquals("", emptyResult.content());
    }

    @Test
    @DisplayName("hasDueDateHint detects date keywords")
    void testDueDateHints() {
        assertTrue(InputParser.hasDueDateHint("Finish report today"));
        assertTrue(InputParser.hasDueDateHint("Submit tomorrow"));
        assertTrue(InputParser.hasDueDateHint("Task due Friday"));
        assertFalse(InputParser.hasDueDateHint("Just a thought"));
    }
}
