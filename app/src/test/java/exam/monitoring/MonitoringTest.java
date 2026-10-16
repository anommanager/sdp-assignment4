package exam.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringTest {
    @TempDir
    Path dir;

    @Test
    void honestStudentIsCleared() {
        new MonitoringStore(dir).check("s1", 3);
        assertEquals(MonitoringRecord.Status.CLEARED, new MonitoringStore(dir).find("s1").status);
    }

    @Test
    void cheaterIsFlaggedAndFlagIsPersisted() {
        assertThrows(CheatingDetectedException.class, () -> new MonitoringStore(dir).check("s1", 4));
        assertEquals(MonitoringRecord.Status.FLAGGED, new MonitoringStore(dir).find("s1").status);
    }

    @Test
    void onlyClearedCanBeVoided() {
        MonitoringStore b = new MonitoringStore(dir);
        b.check("ok", 0);
        b.voidRecord("ok");
        assertEquals(MonitoringRecord.Status.VOIDED, b.find("ok").status);
        assertThrows(IllegalStateException.class, () -> b.voidRecord("ok"));
    }

    @Test
    void flaggedRecordCannotBeCleared() {
        MonitoringRecord r = new MonitoringRecord("s1", MonitoringRecord.Status.FLAGGED);
        assertThrows(IllegalStateException.class, r::clear);
        assertThrows(IllegalStateException.class, r::voidIt);
    }

    @Test
    void detectorThreshold() {
        CheatingDetector d = new CheatingDetector();
        assertFalse(d.isCheating(3));
        assertTrue(d.isCheating(4));
    }

    @Test
    void checkOnExistingClearedThrows() {
        MonitoringStore b = new MonitoringStore(dir);
        b.check("s1", 0);
        assertThrows(IllegalStateException.class, () -> b.check("s1", 1));
    }

    @Test
    void checkOnExistingFlaggedThrows() {
        MonitoringStore b = new MonitoringStore(dir);
        assertThrows(CheatingDetectedException.class, () -> b.check("s1", 9));
        assertThrows(IllegalStateException.class, () -> b.check("s1", 0));
    }

    @Test
    void timerDetectsExpiredSession() {
        ExamTimer t = new ExamTimer();
        long threeHoursAgo = System.currentTimeMillis() - 3 * 60 * 60_000L;
        assertTrue(t.isExpired(threeHoursAgo, ExamTimer.DEFAULT_LIMIT_MINUTES));
        assertFalse(t.isExpired(System.currentTimeMillis(), ExamTimer.DEFAULT_LIMIT_MINUTES));
        assertEquals(0, t.minutesRemaining(threeHoursAgo, ExamTimer.DEFAULT_LIMIT_MINUTES));
        assertTrue(t.minutesRemaining(System.currentTimeMillis(), ExamTimer.DEFAULT_LIMIT_MINUTES) > 0);
    }

    @Test
    void detectorChecksTimeExpiry() {
        CheatingDetector d = new CheatingDetector();
        long threeHoursAgo = System.currentTimeMillis() - 3 * 60 * 60_000L;
        assertTrue(d.isTimeExpired(threeHoursAgo));
        assertFalse(d.isTimeExpired(System.currentTimeMillis()));
    }

    @Test
    void checkThrowsOnExpiredSessionAndSavesNothing() {
        MonitoringStore b = new MonitoringStore(dir);
        long threeHoursAgo = System.currentTimeMillis() - 3 * 60 * 60_000L;
        assertThrows(TimeExpiredException.class, () -> b.check("s1", 0, threeHoursAgo));
        assertNull(b.find("s1"));
    }
}
