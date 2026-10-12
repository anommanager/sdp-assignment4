package exam.monitoring;

public class CheatingDetectedException extends RuntimeException {
  public CheatingDetectedException(int tabSwitches) {
    super("Cheating: " + tabSwitches + " tab switches");
  }
}
