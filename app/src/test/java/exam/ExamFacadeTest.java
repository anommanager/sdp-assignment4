package exam;

import exam.identity.SessionStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExamFacadeTest extends ExamDsl {

  @Test
  void happyPath_allThreeStepsCommitted() {
    String id = honestStudentTakesExam();
    assertSavedState(id, "flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED");
    assertNull(exams().getFailureReason(id));
  }

  @Test
  void bFailsAfterA_onlyAIsCompensated() {
    String id = cheaterTakesExam();
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=FLAGGED C=-");
    assertEquals("B failed: Cheating: 9 tab switches", exams().getFailureReason(id));
  }

  @Test
  void cFailsAfterAandB_bThenAAreCompensated() {
    String id = studentGetsBrokenScore();
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=VOIDED C=-");
    assertEquals("C failed: Invalid score: 150", exams().getFailureReason(id));
  }

  @Test
  void aFails_badStudentId_nothingSaved() {
    assertThrows(ExamException.class, () -> exams().takeExam("bob", 0, 90));
    assertTrue(exams().ids().isEmpty());
  }

  @Test
  void aFails_negativeTabSwitches_nothingSaved() {
    assertThrows(ExamException.class, () -> exams().takeExam("S1234", -1, 90));
    assertTrue(exams().ids().isEmpty());
  }

  @Test
  void restartAfterA_resumeFinishesChainFromDisk() {
    String id = honestStudentOnlyPassedIdentity();
    assertSavedState(id, "flow=PENDING_B A=VERIFIED B=- C=-");
    assertEquals(ExamFacade.Status.COMPLETED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED");
  }

  @Test
  void crashAfterBCommitted_resumeTrustsBsSavedRecord() {
    String id = honestStudentOnlyPassedIdentity();
    monitoring().check(id, 0);
    assertEquals(ExamFacade.Status.COMPLETED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED");
  }

  @Test
  void crashAfterBFlagged_resumeStillCompensatesA() {
    String id = exams().start("S1234", 9, 90);
    assertThrows(RuntimeException.class, () -> monitoring().check(id, 9));
    assertEquals(ExamFacade.Status.COMPENSATED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=FLAGGED C=-");
  }

  @Test
  void crashAfterCCommitted_resumeOnlyMarksCompleted() {
    String id = honestStudentOnlyPassedIdentity();
    monitoring().check(id, 0);
    exams().setStatus(id, ExamFacade.Status.PENDING_C);
    grading().record(id, 90);
    assertEquals(ExamFacade.Status.COMPLETED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED");
  }

  @Test
  void crashMidCompensation_resumeDoesNotUndoTwice() {
    String id = exams().start("S1234", 0, 150);
    monitoring().check(id, 0);
    exams().setStatus(id, ExamFacade.Status.PENDING_C);
    monitoring().voidRecord(id);
    assertEquals(ExamFacade.Status.COMPENSATED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPENSATED A=CANCELLED B=VOIDED C=-");
  }

  @Test
  void resumeOnFinishedExamChangesNothing() {
    String id = honestStudentTakesExam();
    assertEquals(ExamFacade.Status.COMPLETED, afterRestart().resume(id));
    assertSavedState(id, "flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED");
  }

  @Test
  void unknownIdIsReported() {
    assertThrows(ExamException.class, () -> exams().resume("nope"));
  }

  @Test
  void subsystemsAreUntouchedByFacadeWhenUsedDirectly() {
    String id = new SessionStore(dir).start("S1234").id;
    assertThrows(ExamException.class, () -> exams().getStatus(id));
  }
}
