package exam;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminFacadeTest extends ExamDsl {

  @Test
  void revokeCompletedExam_undoesCThenBThenA() {
    String id = honestStudentTakesExam();
    admin().revoke(id);
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=VOIDED C=REVOKED");
  }

  @Test
  void revokeTwiceIsRejected() {
    String id = honestStudentTakesExam();
    admin().revoke(id);
    assertThrows(ExamException.class, () -> admin().revoke(id));
  }

  @Test
  void revokeAfterA_undoesOnlyA() {
    String id = honestStudentOnlyPassedIdentity();
    admin().revoke(id);
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=- C=-");
  }

  @Test
  void revokeAfterB_undoesBAndA() {
    String id = honestStudentOnlyPassedIdentity();
    monitoring().check(id, 0);
    exams().setStatus(id, ExamFacade.Status.PENDING_C);
    admin().revoke(id);
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=VOIDED C=-");
  }

  @Test
  void listAllShowsEveryPersistedExam() {
    String ok = honestStudentTakesExam();
    String bad = cheaterTakesExam();
    List<String> rows = admin().listAll();
    assertEquals(2, rows.size());
    assertTrue(rows.contains(ok + "  flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED"));
    assertTrue(rows.contains(bad + "  flow=COMPENSATED A=CANCELLED B=FLAGGED C=-"));
  }

  @Test
  void inspectUnknownIdIsReported() {
    assertThrows(ExamException.class, () -> admin().inspect("nope"));
  }

  @Test
  void flagPlagiarismAndResumeCompensatesChain() {
    String id = honestStudentOnlyPassedIdentity();
    admin().flagPlagiarism(id);
    exams().resume(id);
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=VOIDED C=-");
  }
}
