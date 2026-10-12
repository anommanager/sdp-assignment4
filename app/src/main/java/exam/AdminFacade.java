package exam;

import exam.grading.Grade;
import exam.grading.TranscriptStore;
import exam.identity.Session;
import exam.identity.SessionStore;
import exam.monitoring.MonitoringRecord;
import exam.monitoring.MonitoringStore;

import java.util.List;

public class AdminFacade {
  private final ExamFacade exams;
  private final SessionStore a;
  private final MonitoringStore b;
  private final TranscriptStore c;

  public AdminFacade(ExamFacade exams, SessionStore a, MonitoringStore b, TranscriptStore c) {
    this.exams = exams;
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public String inspect(String id) {
    ExamFacade.Status flow = exams.getStatus(id);
    MonitoringRecord m = b.find(id);
    Grade g = c.find(id);
    return "flow=" + flow
        + " A=" + a.find(id).status
        + " B=" + (m == null ? "-" : m.status)
        + " C=" + (g == null ? "-" : g.status);
  }

  public List<String> listAll() {
    return exams.ids().stream().map(id -> id + "  " + inspect(id)).toList();
  }

  public void revoke(String id) {
    if (exams.getStatus(id) == ExamFacade.Status.COMPENSATED)
      throw new ExamException("Already compensated: " + id);

    Grade g = c.find(id);
    if (g != null && g.status == Grade.Status.RECORDED)
      c.revoke(id);
    MonitoringRecord m = b.find(id);
    if (m != null && m.status == MonitoringRecord.Status.CLEARED)
      b.voidRecord(id);
    if (a.find(id).status == Session.Status.VERIFIED)
      a.cancel(id);
    exams.setStatus(id, ExamFacade.Status.COMPENSATED);
  }
}
