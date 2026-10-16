package exam.monitoring;

public class ExamTimer {
    public static final int DEFAULT_LIMIT_MINUTES = 120;

    public boolean isExpired(long startTimeMs, int limitMinutes) {
        return System.currentTimeMillis() - startTimeMs > (long) limitMinutes * 60_000;
    }

    public int minutesElapsed(long startTimeMs) {
        return (int) ((System.currentTimeMillis() - startTimeMs) / 60_000);
    }

    public int minutesRemaining(long startTimeMs, int limitMinutes) {
        return Math.max(0, limitMinutes - minutesElapsed(startTimeMs));
    }
}
