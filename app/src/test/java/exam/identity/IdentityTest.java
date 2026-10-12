package exam.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IdentityTest {
  @TempDir
  Path dir;

  @Test
  void validStudentIsVerifiedAndSaved() {
    String id = new SessionStore(dir).start("S1234").id;
    Session reloaded = new SessionStore(dir).find(id);
    assertEquals(Session.Status.VERIFIED, reloaded.status);
    assertEquals("S1234", reloaded.studentId);
  }

  @Test
  void invalidStudentIsRejectedAndNothingSaved() {
    assertThrows(InvalidStudentException.class, () -> new SessionStore(dir).start("bob"));
    assertThrows(InvalidStudentException.class, () -> new SessionStore(dir).start(null));
  }

  @Test
  void unknownSessionIsNull() {
    assertNull(new SessionStore(dir).find("nope"));
  }

  @Test
  void cancelOnlyOnce() {
    SessionStore a = new SessionStore(dir);
    String id = a.start("S1234").id;
    a.cancel(id);
    assertEquals(Session.Status.CANCELLED, new SessionStore(dir).find(id).status);
    assertThrows(IllegalStateException.class, () -> a.cancel(id));
  }

  @Test
  void cannotVerifyTwice() {
    assertThrows(IllegalStateException.class,
        () -> new Session("x", "S1234", Session.Status.VERIFIED).verify());
  }

  @Test
  void newSessionCannotBeCancelled() {
    assertThrows(IllegalStateException.class,
        () -> new Session("x", "S1234", Session.Status.NEW).cancel());
  }
}
