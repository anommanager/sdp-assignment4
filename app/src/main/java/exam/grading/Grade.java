package exam.grading;

public class Grade {
  public enum Status {
    NEW, RECORDED, REVOKED
  }

  public final String sessionId;
  public final int score;
  public Status status;

  public Grade(String sessionId, int score, Status status) {
    this.sessionId = sessionId;
    this.score = score;
    this.status = status;
  }

  public void record() {
    if (status != Status.NEW)
      throw new IllegalStateException("Cannot record " + status + " grade");
    status = Status.RECORDED;
  }

  public void revoke() {
    if (status != Status.RECORDED)
      throw new IllegalStateException("Cannot revoke " + status + " grade");
    status = Status.REVOKED;
  }
}
