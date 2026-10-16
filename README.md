# Assignment 4: Facade, Exam Proctoring (variant 12)

A student takes an exam in three steps. Step A is identity, step B is monitoring, step C – grading.

Identity is in the `exam.identity` package. It checks the student id. A correct id matches the pattern `S1234`. After a successful check a session is opened.

Monitoring is in the `exam.monitoring` package. It counts tab switches. More than 3 switches means cheating. It also tracks the time: an exam can last for 120 min at maximum. `ExamTimer` measures the time.

Grading is in the `exam.grading` package. It checks if a session was plagiarized. After the check the score is recorded.

Each subsystem has its own store, rule class, entity with status and exceptions. All stores write their own json file. Subsystems are isolated: they don't know about each other. Only the facades use all three.

## To run

To run the demo use `./gradlew run`. To test use `./gradlew test`. The test command also generates the coverage report in `app/build/reports/jacoco/test/html`.

To reset the saved data use `./gradlew run --args=--reset`. Alternatively, you can delete the `app/data` folder.

## Demo

Menu item 1 is the whole exam: it asks for an id, number of tab switches and a score. Normal run: `S1234`, `0`, `90`. Result: `flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED`.

A fail in step B: `S1234`, `9`, `90`. The result: `flow=COMPENSATED A=CANCELLED B=FLAGGED C=-`. Only step A is rolled back.

A fail in step C: menu item 2 asks for the same input but runs only step A. Let's use `S1234`, `0`, `90`. It prints the exam id. Then use item 7 with this id to mark the session as plagiarized. Use item 3 with this id: the result is `flow=COMPENSATED A=CANCELLED B=VOIDED C=-`. Step B is rolled back first, then step A.

A fail in step B due to late finish: step B also fails if more than 120 min have passed. Start an exam with item 2: `S1234`, `0`, `90`. Then resume it with item 3 more than 120 min later. The result: `flow=COMPENSATED A=CANCELLED B=- C=-`. Only step A is rolled back.

Invalid input: provide `bob` as an id or `150` as a score: an error message is shown. No data is saved.

Resuming after restart: start an exam with item 2 and remember the printed id. Then exit and re-launch the app. Item 3 with this id resumes the exam from where it stopped.

Other items: item 4 – show one exam, item 5 – show all exams, item 6 – undo an exam manually by its id.

## Facades

`ExamFacade` is the entry point for students: `takeExam()` runs the exam steps, `resume()` continues an exam and saves its status after each step. On fail it rolls back the completed steps in reverse order.

`AdminFacade` is the entry point for admins: it shows the exams, undoes all completed steps of an exam manually and marks sessions as plagiarized.

`Exams.open(dir)` creates the facades. `Main` only uses the facades provided by `Exams`.

## Saved state

The data is stored in `app/data/`. Monitoring records are in `monitoring.json`, sessions – in `sessions.json`, grades – in `transcript.json`, plagiarism marks – in `plagiarism.json`.

The progress of each exam is stored in `flows.json`. It keeps the status, the input values and the time when the exam was started. The status shows which steps are done and which are pending: PENDING_B (A done), PENDING_C (A and B done), COMPLETED, COMPENSATED.

Before each decision the facade also reads the saved status of each subsystem. Thus, a crash at any point is safe: the facade sees on disk which steps were completed before the crash. Once an exam is finished (COMPLETED or COMPENSATED), it is never processed again. Only the completed steps are rolled back.

## Least Knowledge

Without a facade the client code talks to each store directly. The client is aware of the order of steps: first start a session, then check it, then record a score.

The client also has to handle the rollbacks: compensate each step if any later step has failed.

```java
Session s = sessions.start("S1234");
try {
  monitoring.check(s.id, tabSwitches, startedAt);
} catch (CheatingDetectedException | TimeExpiredException e) {
  sessions.cancel(s.id);
  return;
}
try {
  transcript.record(s.id, score);
} catch (PlagiarismException e) {
  if (monitoring.find(s.id).status == MonitoringRecord.Status.CLEARED)
    monitoring.voidRecord(s.id);
  if (sessions.find(s.id).status == Session.Status.VERIFIED)
    sessions.cancel(s.id);
}
```

The client code above has many dependencies: three stores, three entities and three exceptions. It also has to navigate long chains to find the status, such as `monitoring.find(id).status`.

With a facade the client only talks to one object:

```java
String id = exams.takeExam("S1234", tabSwitches, score);
```

## Facade vs Adapter

Adapters and Facades have different purposes. An Adapter is used to convert one interface to another. The client code expects to use a particular interface. A Facade provides a simplified interface to a subsystem.

My project does not need Adapters. All stores already have clear methods: start, check, record, cancel, etc. No client code expects a different interface from these stores.

My task was to coordinate these stores. The single action "take an exam" requires calling three stores in a certain order and compensating them if any step has failed. For example, cheating in `MonitoringStore` cancels only the session in `SessionStore`, but plagiarism in `TranscriptStore` voids the monitoring record first and then cancels the session. An Adapter is not helpful here: the client would still have to know the order of steps, handle all exceptions and rollbacks. Thus, `ExamFacade.takeExam(...)` centralizes this logic. The same applies to `AdminFacade`: it hides the complexity of the manual undo.

## Tests

`IdentityTest`, `MonitoringTest` and `GradingTest` test each subsystem in isolation. `MonitoringTest` also tests the time limit. `ExamFacadeTest` tests the normal flow, both failures and resuming after a crash in the middle of a chain. `AdminFacadeTest` tests the manual undo after each step and marking the session as plagiarized. `ExamDsl` contains helpers: `cheaterTakesExam()`, `plagiaristTakesExam()` and `assertSavedState(...)`. With these helpers a test reads like a sentence about the exam domain.

Each test uses its own `@TempDir`: the tests are isolated. The coverage is measured with JaCoCo. `./gradlew check` fails if the line coverage is less than 80%. The Main class is excluded: it only contains the menu.

## UML

![UML](docs/uml.svg)
