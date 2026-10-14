package exam.monitoring;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class MonitoringStore {
  private final Path file;
  private final CheatingDetector detector = new CheatingDetector();
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public MonitoringStore(Path dataDir) {
    file = dataDir.resolve("monitoring.json");
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
    JsonElement el = load().get(sessionId);
    return el == null ? null : new MonitoringRecord(sessionId, MonitoringRecord.Status.valueOf(el.getAsString()));
  }

  private void save(MonitoringRecord r) {
    JsonObject root = load();
    root.addProperty(r.sessionId, r.status.name());
    write(root);
  }

  private JsonObject load() {
    try (Reader r = Files.newBufferedReader(file)) {
      JsonObject obj = GSON.fromJson(r, JsonObject.class);
      return obj != null ? obj : new JsonObject();
    } catch (IOException noFileYet) {
      return new JsonObject();
    }
  }

  private void write(JsonObject root) {
    try {
      Files.createDirectories(file.getParent());
      try (Writer w = Files.newBufferedWriter(file)) {
        GSON.toJson(root, w);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
