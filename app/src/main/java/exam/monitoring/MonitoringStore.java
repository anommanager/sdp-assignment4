package exam.monitoring;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class MonitoringStore {
  private final Path file;
  private final CheatingDetector detector = new CheatingDetector();

  public MonitoringStore(Path dataDir) {
    file = dataDir.resolve("monitoring.properties");
  }

  public void check(String sessionId, int tabSwitches) {
    MonitoringRecord r = new MonitoringRecord(sessionId, MonitoringRecord.Status.PENDING);
    if (detector.isCheating(tabSwitches)) {
      r.flag();
      save(r);
      throw new CheatingDetectedException(tabSwitches);
    }
    r.clear();
    save(r);
  }

  public void voidRecord(String sessionId) {
    MonitoringRecord r = find(sessionId);
    r.voidIt();
    save(r);
  }

  public MonitoringRecord find(String sessionId) {
    String status = load().getProperty(sessionId);
    return status == null ? null : new MonitoringRecord(sessionId, MonitoringRecord.Status.valueOf(status));
  }

  private void save(MonitoringRecord r) {
    Properties p = load();
    p.setProperty(r.sessionId, r.status.name());
    try {
      Files.createDirectories(file.getParent());
      try (Writer w = Files.newBufferedWriter(file)) {
        p.store(w, null);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Properties load() {
    Properties p = new Properties();
    try (Reader r = Files.newBufferedReader(file)) {
      p.load(r);
    } catch (IOException noFileYet) {
    }
    return p;
  }
}
