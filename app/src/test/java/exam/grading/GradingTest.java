package exam.grading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GradingTest {
    @TempDir
    Path dir;

    @Test
    void validScoreIsRecorded() {
        new TranscriptStore(dir).record("s1", 90);
        Grade g = new TranscriptStore(dir).find("s1");
        assertEquals(Grade.Status.RECORDED, g.status);
        assertEquals(90, g.score);
    }

    @Test
    void invalidScoreSavesNothing() {
        TranscriptStore c = new TranscriptStore(dir);
        assertThrows(InvalidScoreException.class, () -> c.record("s1", 150));
        assertThrows(InvalidScoreException.class, () -> c.record("s1", -1));
        assertNull(c.find("s1"));
    }

    @Test
    void revokeOnlyOnce() {
        TranscriptStore c = new TranscriptStore(dir);
        c.record("s1", 50);
        c.revoke("s1");
        assertEquals(Grade.Status.REVOKED, new TranscriptStore(dir).find("s1").status);
        assertThrows(IllegalStateException.class, () -> c.revoke("s1"));
    }

    @Test
    void revokedGradeCannotBeRecordedAgain() {
        assertThrows(IllegalStateException.class, () -> new Grade("x", 1, Grade.Status.REVOKED).record());
    }

    @Test
    void engineBoundaries() {
        GradingEngine e = new GradingEngine();
        assertTrue(e.isValid(0));
        assertTrue(e.isValid(100));
        assertFalse(e.isValid(101));
    }

    @Test
    void recordOnRecordedGradeThrows() {
        TranscriptStore c = new TranscriptStore(dir);
        c.record("s1", 90);
        assertThrows(IllegalStateException.class, () -> c.record("s1", 80));
    }

    @Test
    void recordOnRevokedGradeThrows() {
        TranscriptStore c = new TranscriptStore(dir);
        c.record("s1", 90);
        c.revoke("s1");
        assertThrows(IllegalStateException.class, () -> c.record("s1", 80));
    }
}
