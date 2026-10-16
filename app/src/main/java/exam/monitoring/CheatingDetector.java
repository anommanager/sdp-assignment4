package exam.monitoring;

public class CheatingDetector {
    private final ExamTimer timer = new ExamTimer();

    public boolean isCheating(int tabSwitches) {
        return tabSwitches > 3;
    }

    public boolean isTimeExpired(long startedAt) {
        return timer.isExpired(startedAt, ExamTimer.DEFAULT_LIMIT_MINUTES);
    }
}
