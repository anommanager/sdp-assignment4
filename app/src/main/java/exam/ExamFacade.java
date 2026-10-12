package exam;

import exam.grading.Grade;
import exam.grading.TranscriptStore;
import exam.identity.InvalidStudentException;
import exam.identity.Session;
import exam.identity.SessionStore;
import exam.monitoring.MonitoringRecord;
import exam.monitoring.MonitoringStore;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;

public class ExamFacade {
  public enum Status {
    PENDING_B, PENDING_C, COMPLETED, COMPENSATED
  }

  private final SessionStore a;
  private final MonitoringStore b;
  private final TranscriptStore c;
  private final Path flowFile;

  public ExamFacade(SessionStore a, MonitoringStore b, TranscriptStore c, Path dataDir) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.flowFile = dataDir.resolve("flows.properties");
  }

  public String takeExam(String studentId, int tabSwitches, int score) {
    String id = start(studentId, tabSwitches, score);
    resume(id);
    return id;
  }

  public String start(String studentId, int tabSwitches, int score) {
    if (tabSwitches < 0)
      throw new ExamException("Tab switches cannot be negative: " + tabSwitches);
    String id;
    try {
      id = a.start(studentId).id;
    } catch (InvalidStudentException e) {
      throw new ExamException(e.getMessage());
    }
    write(id, Status.PENDING_B + ";" + tabSwitches + ";" + score);
    return id;
  }

  public Status resume(String id) {
    String[] f = read(id).split(";");
    int tabSwitches = Integer.parseInt(f[1]);
    int score = Integer.parseInt(f[2]);

    if (getStatus(id) == Status.PENDING_B) {
      MonitoringRecord m = b.find(id);
      if (m == null) {
        try {
          b.check(id, tabSwitches);
          setStatus(id, Status.PENDING_C);
        } catch (RuntimeException e) {
          compensate(id, "B failed: " + e.getMessage());
        }
      } else if (m.status == MonitoringRecord.Status.CLEARED) {
        setStatus(id, Status.PENDING_C);
      } else {
        compensate(id, "B failed: monitoring " + m.status);
      }
    }
    if (getStatus(id) == Status.PENDING_C) {
      Grade g = c.find(id);
      if (g != null && g.status == Grade.Status.RECORDED) {
        setStatus(id, Status.COMPLETED);
      } else {
        try {
          c.record(id, score);
          setStatus(id, Status.COMPLETED);
        } catch (RuntimeException e) {
          compensate(id, "C failed: " + e.getMessage());
        }
      }
    }
    return getStatus(id);
  }

  private void compensate(String id, String reason) {
    MonitoringRecord m = b.find(id);
    if (m != null && m.status == MonitoringRecord.Status.CLEARED)
      b.voidRecord(id);
    if (a.find(id).status == Session.Status.VERIFIED)
      a.cancel(id);
    write(id, Status.COMPENSATED + rest(id) + ";" + reason.replace(';', ','));
  }

  public Status getStatus(String id) {
    return Status.valueOf(read(id).split(";")[0]);
  }

  public String getFailureReason(String id) {
    String[] f = read(id).split(";", 4);
    return f.length > 3 ? f[3] : null;
  }

  public List<String> ids() {
    return load().stringPropertyNames().stream().sorted().toList();
  }

  void setStatus(String id, Status status) {
    write(id, status + rest(id));
  }

  private String rest(String id) {
    String[] f = read(id).split(";");
    return ";" + f[1] + ";" + f[2];
  }

  private String read(String id) {
    String line = load().getProperty(id);
    if (line == null)
      throw new ExamException("Unknown exam id: " + id);
    return line;
  }

  private void write(String id, String line) {
    Properties p = load();
    p.setProperty(id, line);
    try {
      Files.createDirectories(flowFile.getParent());
      try (Writer w = Files.newBufferedWriter(flowFile)) {
        p.store(w, null);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Properties load() {
    Properties p = new Properties();
    try (Reader r = Files.newBufferedReader(flowFile)) {
      p.load(r);
    } catch (IOException noFileYet) {
    }
    return p;
  }
}
