package exam.grading;

public class GradingEngine {
  public boolean isValid(int score) {
    return score >= 0 && score <= 100;
  }
}
