# Assignment 4: Facade, Exam Proctoring (variant 12)

A student takes an exam in three steps:

A, Identity (`exam.identity`): checks the student id and opens a session
B, Monitoring (`exam.monitoring`): counts tab switches. More than 3 is cheating.
C, Grading (`exam.grading`): checks the score and saves it.

Each subsystem has a store (saves to a file), a rule class, an entity with a status, plus its own exception.
The subsystems are not aware of each other, only in the facades.

## How to run

```bash
./gradlew run         # demo
./gradlew test        # tests + coverage report
```

To reset the saved data, run `./gradlew run --args=--reset` or delete the folder with `rm -rf app/data`.

## Demo

Pick `1` and enter a student id, tab switches and a score:

| Case | Input | Result |
|---|---|---|
| Normal | `S1234`, `0`, `90` | `flow=COMPLETED A=VERIFIED B=CLEARED C=RECORDED` |
| B fails (cheating) | `S1234`, `9`, `90` | `flow=COMPENSATED A=CANCELLED B=FLAGGED C=-` |
| C fails (bad score) | `S1234`, `0`, `150` | `flow=COMPENSATED A=CANCELLED B=VOIDED C=-` |
| A fails (bad id) | `bob` | error, nothing saved |

Other menu items:

`2` only does step A. Exit and restart the app, then `3` with the id to finish the chain.
`4` shows one exam, `5` list all exams, `6` undo an exam (admin).

## Facades

- `ExamFacade` (student): `takeExam()` takes A→B→C. `resume()` saves the status after each step and undoes the right steps if one fails.
- `AdminFacade` (admin): shows exams and undo one by hand.

## Saved state

Each store has its own file in `app/data/`. The chain status (`PENDING_B`, `PENDING_C`, `COMPLETED`, `COMPENSATED`) is in `flows.properties`.

The facade also checks each subsystem's saved status, so a crash anywhere is safe (never redoes an already completed step, only undoes steps that have been done).

## Least Knowledge

Before, the client knew about all the stores, the order and what to undo:

```java
Session s = sessions.start("S1234");
try {
  monitoring.check(s.id, tabSwitches);
} catch (CheatingDetectedException e) {
  sessions.cancel(s.id);
  return;
}
try {
  transcript.record(s.id, score);
} catch (InvalidScoreException e) {
  if (monitoring.find(s.id).status == MonitoringRecord.Status.CLEARED)
    monitoring.voidRecord(s.id);
  if (sessions.find(s.id).status == Session.Status.VERIFIED)
    sessions.cancel(s.id);
}
```

After, the client has only one friend:

```java
String id = exams.takeExam("S1234", tabSwitches, score);
```

`Main` only uses the two facades.

## Facade vs Adapter

An Adapter makes a class fit an interface that the client already expects.
My stores already have good interfaces, so there is nothing to adapt.

My problem is coordination: "take an exam" uses three subsystems in the right order, and undo if B or C fail. A Facade hides this behind one method. An Adapter would not, the client would need to know the order and the undo rules.

## Tests

- `IdentityTest`, `MonitoringTest`, `GradingTest`: each subsystem on its own.
- `ExamFacadeTest`: normal, both failures, restarts/crashes in mid-chain.
- `AdminFacadeTest`: manual undo at each step.
- `ExamDsl`: helpers like `cheaterTakesExam()` and `assertSavedState(...)` so tests read like sentences.

Each test uses its own `@TempDir`. Line coverage is about 97% (JaCoCo). `Main` is excluded because it is the console menu.

## UML

![UML](docs/uml.svg)
