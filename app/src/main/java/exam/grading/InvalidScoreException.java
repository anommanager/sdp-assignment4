package exam.grading;

public class InvalidScoreException extends IllegalArgumentException {
  public InvalidScoreException(int score) {
    super("Invalid score: " + score);
  }
}
