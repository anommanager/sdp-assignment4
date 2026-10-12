package exam.grading;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class TranscriptStore {
  private final Path file;
  private final GradingEngine engine = new GradingEngine();

  public TranscriptStore(Path dataDir) {
    file = dataDir.resolve("transcript.properties");
  }

  public void record(String sessionId, int score) {
    if (!engine.isValid(score))
      throw new InvalidScoreException(score);
    Grade g = new Grade(sessionId, score, Grade.Status.NEW);
    g.record();
    save(g);
  }

  public void revoke(String sessionId) {
    Grade g = find(sessionId);
    g.revoke();
    save(g);
  }

  public Grade find(String sessionId) {
    String line = load().getProperty(sessionId);
    if (line == null)
      return null;
    String[] f = line.split(";");
    return new Grade(sessionId, Integer.parseInt(f[0]), Grade.Status.valueOf(f[1]));
  }

  private void save(Grade g) {
    Properties p = load();
    p.setProperty(g.sessionId, g.score + ";" + g.status);
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
