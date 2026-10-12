package exam.identity;

public class IdentityVerifier {
  public boolean isValid(String studentId) {
    return studentId != null && studentId.matches("S\\d{4}");
  }
}
