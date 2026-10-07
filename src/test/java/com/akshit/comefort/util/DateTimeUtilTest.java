package com.akshit.comefort.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateTimeUtil Tests")
class DateTimeUtilTest {

    @Test
    @DisplayName("parseFriendlyDate parses 'today' and 'tomorrow'")
    void testParseFriendlyDateKeywords() {
        LocalDate today = DateTimeUtil.parseFriendlyDate("today");
        assertNotNull(today);
        assertEquals(LocalDate.now(), today);

        LocalDate tomorrow = DateTimeUtil.parseFriendlyDate("tomorrow");
        assertNotNull(tomorrow);
        assertEquals(LocalDate.now().plusDays(1), tomorrow);
    }

    @Test
    @DisplayName("parseFriendlyDate parses standard ISO format yyyy-MM-dd")
    void testParseFriendlyDateIso() {
        LocalDate date = DateTimeUtil.parseFriendlyDate("2026-12-25");
        assertNotNull(date);
        assertEquals(2026, date.getYear());
        assertEquals(12, date.getMonthValue());
        assertEquals(25, date.getDayOfMonth());
    }

    @Test
    @DisplayName("parseFriendlyDate returns null for invalid or blank date")
    void testParseFriendlyDateInvalid() {
        assertNull(DateTimeUtil.parseFriendlyDate(null));
        assertNull(DateTimeUtil.parseFriendlyDate(""));
        assertNull(DateTimeUtil.parseFriendlyDate("not-a-date"));
    }

    @Test
    @DisplayName("Storage roundtrip for LocalDateTime")
    void testStorageRoundtripDateTime() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String stored = DateTimeUtil.formatForStorage(now);
        LocalDateTime parsed = DateTimeUtil.parseDateTime(stored);
        assertEquals(now, parsed);
    }

    @Test
    @DisplayName("Storage roundtrip for LocalDate")
    void testStorageRoundtripDate() {
        LocalDate today = LocalDate.now();
        String stored = DateTimeUtil.formatDateForStorage(today);
        LocalDate parsed = DateTimeUtil.parseDate(stored);
        assertEquals(today, parsed);
    }

    @Test
    @DisplayName("relativeTime formats recent timestamps correctly")
    void testRelativeTime() {
        LocalDateTime justNow = LocalDateTime.now();
        assertEquals("just now", DateTimeUtil.relativeTime(justNow));

        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        assertEquals("5 min ago", DateTimeUtil.relativeTime(fiveMinAgo));

        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        assertEquals("2 hours ago", DateTimeUtil.relativeTime(twoHoursAgo));

        assertEquals("—", DateTimeUtil.relativeTime(null));
    }
}
