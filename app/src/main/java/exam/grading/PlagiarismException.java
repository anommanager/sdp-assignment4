package exam.grading;

public class PlagiarismException extends RuntimeException {
    public PlagiarismException(String sessionId) {
        super("Plagiarism detected: " + sessionId);
    }
}
