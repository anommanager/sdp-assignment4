package exam;

import exam.grading.TranscriptStore;
import exam.identity.SessionStore;
import exam.monitoring.MonitoringStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {
  public static void main(String[] args) {
    Path data = Path.of("data");
    if (Arrays.asList(args).contains("--reset")) {
      reset(data);
      System.out.println("Store reset: " + data.toAbsolutePath());
    }
    SessionStore a = new SessionStore(data);
    MonitoringStore b = new MonitoringStore(data);
    TranscriptStore c = new TranscriptStore(data);
    ExamFacade exams = new ExamFacade(a, b, c, data);
    AdminFacade admin = new AdminFacade(exams, a, b, c);
    Scanner in = new Scanner(System.in);

    while (true) {
      System.out.println("\n1) take exam  2) start only (A)  3) resume  4) inspect  5) list all  6) revoke (admin)  0) exit");
      System.out.println("   tab switches > 3 -> B fails | score not 0..100 -> C fails");
      if (!in.hasNextLine())
        return;
      String choice = in.nextLine().trim();
      try {
        switch (choice) {
          case "1", "2" -> {
            System.out.print("student id (S1234): ");
            String student = in.nextLine().trim();
            System.out.print("tab switches: ");
            int tabs = Integer.parseInt(in.nextLine().trim());
            System.out.print("score: ");
            int score = Integer.parseInt(in.nextLine().trim());
            String id = choice.equals("1")
                ? exams.takeExam(student, tabs, score)
                : exams.start(student, tabs, score);
            report(exams, admin, id);
          }
          case "3" -> {
            System.out.print("id: ");
            String id = in.nextLine().trim();
            exams.resume(id);
            report(exams, admin, id);
          }
          case "4" -> {
            System.out.print("id: ");
            report(exams, admin, in.nextLine().trim());
          }
          case "5" -> {
            if (exams.ids().isEmpty())
              System.out.println("(no exams yet)");
            admin.listAll().forEach(System.out::println);
          }
          case "6" -> {
            System.out.print("id: ");
            String id = in.nextLine().trim();
            admin.revoke(id);
            report(exams, admin, id);
          }
          case "0" -> {
            return;
          }
          default -> System.out.println("?");
        }
      } catch (NumberFormatException e) {
        System.out.println("ERROR: not a number");
      } catch (RuntimeException e) {
        System.out.println("ERROR: " + e.getMessage());
      }
    }
  }

  private static void report(ExamFacade exams, AdminFacade admin, String id) {
    System.out.println("id=" + id + " " + admin.inspect(id));
    String reason = exams.getFailureReason(id);
    if (reason != null)
      System.out.println("   compensated because: " + reason);
  }

  private static void reset(Path data) {
    if (!Files.exists(data))
      return;
    try (Stream<Path> files = Files.walk(data)) {
      files.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
