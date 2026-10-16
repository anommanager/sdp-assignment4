package exam.monitoring;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class MonitoringStore {
    private final Path file;
    private final CheatingDetector detector = new CheatingDetector();
    private final ExamTimer timer = new ExamTimer();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public MonitoringStore(Path dataDir) {
        file = dataDir.resolve("monitoring.json");
    }

    public void check(String sessionId, int tabSwitches) {
        check(sessionId, tabSwitches, System.currentTimeMillis());
    }

    public void check(String sessionId, int tabSwitches, long startedAt) {
        if (find(sessionId) != null)
            throw new IllegalStateException("Monitoring record already exists: " + sessionId);
        if (detector.isTimeExpired(startedAt))
            throw new TimeExpiredException();
        MonitoringRecord r = new MonitoringRecord(sessionId, MonitoringRecord.Status.PENDING, startedAt);
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
        JsonObject entry = load().getAsJsonObject(sessionId);
        if (entry == null) return null;
        MonitoringRecord.Status status = MonitoringRecord.Status.valueOf(entry.get("status").getAsString());
        long startedAt = entry.has("startedAt") ? entry.get("startedAt").getAsLong() : 0;
        return new MonitoringRecord(sessionId, status, startedAt);
    }

    public int minutesRemaining(String sessionId) {
        MonitoringRecord r = find(sessionId);
        if (r == null) return ExamTimer.DEFAULT_LIMIT_MINUTES;
        return timer.minutesRemaining(r.startedAt, ExamTimer.DEFAULT_LIMIT_MINUTES);
    }

    private void save(MonitoringRecord r) {
        JsonObject root = load();
        JsonObject entry = new JsonObject();
        entry.addProperty("status", r.status.name());
        entry.addProperty("startedAt", r.startedAt);
        root.add(r.sessionId, entry);
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
