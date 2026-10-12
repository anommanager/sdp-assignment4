package exam.identity;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

public class SessionStore {
  private final Path file;
  private final IdentityVerifier verifier = new IdentityVerifier();

  public SessionStore(Path dataDir) {
    file = dataDir.resolve("sessions.properties");
  }

  public Session start(String studentId) {
    if (!verifier.isValid(studentId))
      throw new InvalidStudentException(studentId);
    Session s = new Session(UUID.randomUUID().toString().substring(0, 8), studentId, Session.Status.NEW);
    s.verify();
    save(s);
    return s;
  }

  public void cancel(String id) {
    Session s = find(id);
    s.cancel();
    save(s);
  }

  public Session find(String id) {
    String line = load().getProperty(id);
    if (line == null)
      return null;
    String[] f = line.split(";");
    return new Session(id, f[0], Session.Status.valueOf(f[1]));
  }

  private void save(Session s) {
    Properties p = load();
    p.setProperty(s.id, s.studentId + ";" + s.status);
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
