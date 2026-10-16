package exam.monitoring;

public class MonitoringRecord {
    public enum Status {
        PENDING, CLEARED, FLAGGED, VOIDED
    }

    public final String sessionId;
    public Status status;
    public final long startedAt;

    public MonitoringRecord(String sessionId, Status status, long startedAt) {
        this.sessionId = sessionId;
        this.status = status;
        this.startedAt = startedAt;
    }

    public MonitoringRecord(String sessionId, Status status) {
        this(sessionId, status, System.currentTimeMillis());
    }

    public void clear() {
        move(Status.PENDING, Status.CLEARED);
    }

    public void flag() {
        move(Status.PENDING, Status.FLAGGED);
    }

    public void voidIt() {
        move(Status.CLEARED, Status.VOIDED);
    }

    private void move(Status from, Status to) {
        if (status != from)
            throw new IllegalStateException("Cannot go " + status + " -> " + to);
        status = to;
    }
}
