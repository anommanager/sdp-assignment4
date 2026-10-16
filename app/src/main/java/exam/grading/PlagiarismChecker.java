package exam.grading;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class PlagiarismChecker {
    private final Path file;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PlagiarismChecker(Path dataDir) {
        this.file = dataDir.resolve("plagiarism.json");
    }

    public void flag(String sessionId) {
        JsonObject root = load();
        root.addProperty(sessionId, true);
        write(root);
    }

    public void check(String sessionId) {
        JsonElement el = load().get(sessionId);
        if (el != null && el.getAsBoolean())
            throw new PlagiarismException(sessionId);
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
