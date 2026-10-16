package exam;

import com.google.gson.*;
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

public class ExamFacade {
    public enum Status {
        PENDING_B, PENDING_C, COMPLETED, COMPENSATED
    }

    private final SessionStore a;
    private final MonitoringStore b;
    private final TranscriptStore c;
    private final Path flowFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ExamFacade(SessionStore a, MonitoringStore b, TranscriptStore c, Path dataDir) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.flowFile = dataDir.resolve("flows.json");
    }

    public String takeExam(String studentId, int tabSwitches, int score) {
        String id = start(studentId, tabSwitches, score);
        resume(id);
        return id;
    }

    public String start(String studentId, int tabSwitches, int score) {
        if (tabSwitches < 0)
            throw new ExamException("Tab switches cannot be negative: " + tabSwitches);
        if (score < 0 || score > 100)
            throw new ExamException("Score out of range: " + score);
        String id;
        try {
            id = a.start(studentId).id;
        } catch (InvalidStudentException e) {
            throw new ExamException(e.getMessage());
        }
        writeEntry(id, Status.PENDING_B, tabSwitches, score, null);
        return id;
    }

    public Status resume(String id) {
        JsonObject entry = readEntry(id);
        int tabSwitches = entry.get("tabSwitches").getAsInt();
        int score = entry.get("score").getAsInt();

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
        JsonObject root = loadAll();
        JsonObject entry = root.getAsJsonObject(id);
        entry.addProperty("status", Status.COMPENSATED.name());
        entry.addProperty("reason", reason);
        writeAll(root);
    }

    public Status getStatus(String id) {
        return Status.valueOf(readEntry(id).get("status").getAsString());
    }

    public String getFailureReason(String id) {
        JsonElement r = readEntry(id).get("reason");
        return r != null && !r.isJsonNull() ? r.getAsString() : null;
    }

    public List<String> ids() {
        return loadAll().keySet().stream().sorted().toList();
    }

    void setStatus(String id, Status status) {
        JsonObject root = loadAll();
        JsonObject entry = root.getAsJsonObject(id);
        if (entry == null)
            throw new ExamException("Unknown exam id: " + id);
        entry.addProperty("status", status.name());
        writeAll(root);
    }

    private JsonObject readEntry(String id) {
        JsonObject entry = loadAll().getAsJsonObject(id);
        if (entry == null)
            throw new ExamException("Unknown exam id: " + id);
        return entry;
    }

    private void writeEntry(String id, Status status, int tabSwitches, int score, String reason) {
        JsonObject root = loadAll();
        JsonObject entry = new JsonObject();
        entry.addProperty("status", status.name());
        entry.addProperty("tabSwitches", tabSwitches);
        entry.addProperty("score", score);
        if (reason != null)
            entry.addProperty("reason", reason);
        root.add(id, entry);
        writeAll(root);
    }

    private JsonObject loadAll() {
        try (Reader r = Files.newBufferedReader(flowFile)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            return obj != null ? obj : new JsonObject();
        } catch (IOException noFileYet) {
            return new JsonObject();
        }
    }

    private void writeAll(JsonObject root) {
        try {
            Files.createDirectories(flowFile.getParent());
            try (Writer w = Files.newBufferedWriter(flowFile)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
