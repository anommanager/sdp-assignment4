package exam.monitoring;

public class TimeExpiredException extends RuntimeException {
    public TimeExpiredException() {
        super("Exam time expired");
    }
}
