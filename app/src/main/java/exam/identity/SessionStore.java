package exam.identity;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class SessionStore {
  private final Path file;
  private final IdentityVerifier verifier = new IdentityVerifier();
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public SessionStore(Path dataDir) {
    file = dataDir.resolve("sessions.json");
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
    JsonObject entry = load().getAsJsonObject(id);
    if (entry == null)
      return null;
    String studentId = entry.get("studentId").getAsString();
    Session.Status status = Session.Status.valueOf(entry.get("status").getAsString());
    return new Session(id, studentId, status);
  }

  private void save(Session s) {
    JsonObject root = load();
    JsonObject entry = new JsonObject();
    entry.addProperty("studentId", s.studentId);
    entry.addProperty("status", s.status.name());
    root.add(s.id, entry);
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
