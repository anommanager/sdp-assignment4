package exam;

import exam.grading.TranscriptStore;
import exam.identity.SessionStore;
import exam.monitoring.MonitoringStore;

import java.nio.file.Path;

public class Exams {
    public final ExamFacade exams;
    public final AdminFacade admin;

    private Exams(ExamFacade exams, AdminFacade admin) {
        this.exams = exams;
        this.admin = admin;
    }

    public static Exams open(Path dataDir) {
        SessionStore a = new SessionStore(dataDir);
        MonitoringStore b = new MonitoringStore(dataDir);
        TranscriptStore c = new TranscriptStore(dataDir);
        ExamFacade exams = new ExamFacade(a, b, c, dataDir);
        AdminFacade admin = new AdminFacade(exams, a, b, c);
        return new Exams(exams, admin);
    }
}
