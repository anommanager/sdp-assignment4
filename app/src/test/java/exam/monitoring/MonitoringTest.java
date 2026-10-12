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
}
