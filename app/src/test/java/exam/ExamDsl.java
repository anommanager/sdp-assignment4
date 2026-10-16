package exam;

import exam.grading.PlagiarismChecker;
import exam.grading.TranscriptStore;
import exam.identity.SessionStore;
import exam.monitoring.MonitoringStore;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class ExamDsl {
    @TempDir
    Path dir;

    ExamFacade exams() {
        return new ExamFacade(identity(), monitoring(), grading(), dir);
    }

    AdminFacade admin() {
        return new AdminFacade(exams(), identity(), monitoring(), grading());
    }

    ExamFacade afterRestart() {
        return exams();
    }

    SessionStore identity() {
        return new SessionStore(dir);
    }

    MonitoringStore monitoring() {
        return new MonitoringStore(dir);
    }

    TranscriptStore grading() {
        return new TranscriptStore(dir);
    }

    PlagiarismChecker plagiarism() {
        return new PlagiarismChecker(dir);
    }

    String honestStudentTakesExam() {
        return exams().takeExam("S1234", 0, 90);
    }

    String cheaterTakesExam() {
        return exams().takeExam("S1234", 9, 90);
    }

    String plagiaristTakesExam() {
        String id = exams().start("S1234", 0, 90);
        plagiarism().flag(id);
        exams().resume(id);
        return id;
    }

    String honestStudentOnlyPassedIdentity() {
        return exams().start("S1234", 0, 90);
    }

    void assertSavedState(String id, String expected) {
        assertEquals(expected, admin().inspect(id));
    }
}
