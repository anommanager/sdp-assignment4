package exam.grading;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class TranscriptStore {
    private final Path file;
    private final GradingEngine engine = new GradingEngine();
    private final PlagiarismChecker plagiarismChecker;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public TranscriptStore(Path dataDir) {
        file = dataDir.resolve("transcript.json");
        plagiarismChecker = new PlagiarismChecker(dataDir);
    }

    public void record(String sessionId, int score) {
        if (find(sessionId) != null)
            throw new IllegalStateException("Grade already exists for: " + sessionId);
        plagiarismChecker.check(sessionId);
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
        JsonObject entry = load().getAsJsonObject(sessionId);
        if (entry == null) return null;
        int score = entry.get("score").getAsInt();
        Grade.Status status = Grade.Status.valueOf(entry.get("status").getAsString());
        return new Grade(sessionId, score, status);
    }

    private void save(Grade g) {
        JsonObject root = load();
        JsonObject entry = new JsonObject();
        entry.addProperty("score", g.score);
        entry.addProperty("status", g.status.name());
        root.add(g.sessionId, entry);
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
