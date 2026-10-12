package exam.identity;

public class InvalidStudentException extends IllegalArgumentException {
  public InvalidStudentException(String studentId) {
    super("Invalid student id: " + studentId);
  }
}
