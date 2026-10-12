package exam.identity;

public class Session {
  public enum Status {
    NEW, VERIFIED, CANCELLED
  }

  public final String id;
  public final String studentId;
  public Status status;

  public Session(String id, String studentId, Status status) {
    this.id = id;
    this.studentId = studentId;
    this.status = status;
  }

  public void verify() {
    if (status != Status.NEW)
      throw new IllegalStateException("Cannot verify " + status + " session");
    status = Status.VERIFIED;
  }

  public void cancel() {
    if (status != Status.VERIFIED)
      throw new IllegalStateException("Cannot cancel " + status + " session");
    status = Status.CANCELLED;
  }
}
