# OOP Final Project Report
## University Information System

---

## 1. Project Goal

Build a console-based university management system that models all major academic roles and their interactions. The system supports student enrollment, grade recording, research activities, complaint and request processing, news management, and messaging - all persisted to disk between sessions.

---

## 2. System Overview

The system is a multi-role CLI application. Each session starts with a login prompt. After authentication, the user gets a role-specific menu. All data is saved to binary files (Java serialization) after every state-changing operation.

### Package Structure

| Package | Contents |
|---|---|
| `Actors` | User, Student, Teacher, Employee, Manager, Dean, Rector, Admin, Signatory |
| `Models` | Course, Mark, Lesson, Message, News, Request, ResearchPaper, ResearchProfile, ResearchProject |
| `Services` | Services (facade), UserRepository, CourseService, MarkService, ResearchService, ReportService, NewsService, MessageService, RequestService, Logger, MenuHelper, Session |
| `Interfaces` | IResearcher |
| `Enums` | Major, YearLevel, School, TeacherTitle, ManagerType, LessonType, CourseStatus, RequestType, RequestStatus, LogEventType |
| `Exceptions` | CreditLimitExceededException, TooManyFailsException, LowHIndexException, NotAResearcherException |

Total: 46 Java source files.

---

## 3. UML Class Diagram (Actor Hierarchy)

```
User (abstract)
 +-- Student          (implements IResearcher)
 +-- Admin
 +-- Signatory (abstract)
 |    +-- Dean
 |    +-- Rector
 +-- Employee         (implements IResearcher)
      +-- Teacher
      +-- Manager
```

`IResearcher` is a mixin interface implemented independently by `Student` and `Employee`. This allows both hierarchies to publish papers and join research projects without forcing a common superclass.

### Key Model Relations

```
Course 1---* Lesson
Course *---* Student  (via Student.courses list)
Course *---* Teacher  (via Course.instructorUsernames)
MarkService stores: Map<"username:courseId", Mark>
ResearchProfile 1---* ResearchPaper
ResearchProfile 1---* projectId (String refs to ResearchProject)
ResearchProject 1---* ResearchPaper
```

---

## 4. Key Classes

### 4.1 User (abstract)

`User` is the root of the actor hierarchy. It holds credentials and implements the **Template Method** pattern for the menu loop.

```java
public final void showMenu(Scanner in, Services services) {
    while (true) {
        printRoleSpecificMenu();          // abstract -- subclass fills role items
        String choice = in.nextLine().trim().toLowerCase();
        switch (choice) {
            case "p" -> manageProfile(in, services);
            case "n" -> viewNews(services);
            // ... shared items
            default -> handleRoleSpecificChoice(choice, in, services); // abstract
        }
    }
}

protected abstract void printRoleSpecificMenu();
protected abstract boolean handleRoleSpecificChoice(String choice, Scanner in, Services services);
```

`User` also contains `manageResearch()` and six helper methods (publish paper, create/join project, view projects, update h-index) shared by every role that implements `IResearcher`. The methods access the interface via `(IResearcher) this`; this cast is always safe because they are only reachable when `isResearcher()` is `true`. Researcher status is granted and revoked exclusively by `Admin`.

Password is stored as SHA-256 hash. Plain-text input is hashed on every login attempt and compared.

```java
private String hash(String input) {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
}
```

### 4.2 Student

`Student` holds a `Major`, `YearLevel`, enrolled `Course` list, and optionally a `ResearchProfile`. Key business constraints enforced at the model level:

- Credit limit: 21 credits. Throwing `CreditLimitExceededException` on overflow.
- Research supervisor: only Year 4, supervisor h-index >= 3, throws `LowHIndexException` otherwise.

```java
public void addCourse(Course course) {
    if (enrolledCredits + course.getCredits() > MAX_CREDITS)
        throw new CreditLimitExceededException(
            course.getName(), course.getCredits(), enrolledCredits, MAX_CREDITS);
    if (!courses.contains(course)) {
        courses.add(course);
        enrolledCredits += course.getCredits();
    }
}
```

Registration is a two-step flow: student submits a request, manager approves it. Approved courses are then added to the student's list.

### 4.3 Mark

`Mark` encodes the KBTU grading system: ATT1 (0-30), ATT2 (0-30), Final (0-40). Entry into the final exam requires ATT1 + ATT2 >= 30.

```java
public void setFinalExam(double score) {
    if (score < 0 || score > MAX_FINAL)
        throw new IllegalArgumentException("Final must be 0-40");
    if (score > 0 && firstAttestation + secondAttestation < MIN_ATT_SUM)
        throw new IllegalStateException("Cannot enter final: attestation total below 30.");
    finalExam = score;
}

public boolean isPassed() {
    return (firstAttestation + secondAttestation) >= 30
        && finalExam >= 20
        && getTotal() >= 50;
}
```

Grade letters: A >= 90, B >= 80, C >= 70, D >= 50, F < 50.

### 4.4 Course

`Course` holds `Lesson` list, instructor usernames, majors, year levels, and registration status. It implements a `readObject()` migration guard so older serialized files (without the `lessons` field) do not cause `NullPointerException`:

```java
private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (lessons == null) lessons = new ArrayList<>();
    if (majors == null) majors = new LinkedHashSet<>();
}
```

### 4.5 IResearcher (interface)

`IResearcher` is a mixin across `Student` and `Employee`. It defines researcher capabilities through default methods that safely delegate to `ResearchProfile`, returning neutral values when the profile is absent (no null-checks needed at call sites).

```java
public interface IResearcher {
    ResearchProfile getResearchProfile();
    void setResearchProfile(ResearchProfile profile);

    default boolean isResearcher() { return getResearchProfile() != null; }
    default int getHIndex() { return isResearcher() ? getResearchProfile().getHIndex() : 0; }
    default List<ResearchPaper> getPapers() {
        return isResearcher() ? getResearchProfile().getPapers() : Collections.emptyList();
    }
    default void addResearchPaper(ResearchPaper paper) {
        requireResearcher();
        getResearchProfile().addPaper(paper);
    }
}
```

### 4.6 Services (Facade)

`Services` groups all service singletons and exposes them through a single object. Every actor receives one `Services` reference; no actor directly instantiates a service.

```java
public class Services {
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final MarkService markService;
    // ... 6 more services + Logger

    public void saveAll() {
        userRepository.save();
        courseService.save();
        markService.save();
        // ...
    }
}
```

### 4.7 Logger (Singleton)

`Logger` writes timestamped entries to `data/logs.txt`. The instance is created once per JVM run.

```java
public static Logger getInstance() {
    if (instance == null) instance = new Logger();
    return instance;
}

public void log(LogEventType type, String details) {
    String line = String.format("[%s] [%-15s] %s",
        LocalDateTime.now().format(FMT), type, details);
    writer.println(line);
}
```

---

## 5. Design Patterns

### 5.1 Template Method

`User.showMenu()` is `final`. It handles the menu loop, common items (profile, news, messages, search), and logout. Role-specific items are injected by two abstract hooks:

- `printRoleSpecificMenu()` -- prints role items
- `handleRoleSpecificChoice()` -- handles them

Every subclass (`Student`, `Teacher`, `Manager`, etc.) overrides only these two methods. Adding a new role requires no changes to the loop logic.

### 5.2 Singleton

`Logger.getInstance()` guarantees a single file writer for the entire session. This prevents interleaved file handles and duplicate log entries.

### 5.3 Facade

`Services` hides the construction and wiring of all service objects. Actors call `services.getMarkService().setMark(...)` instead of knowing where `MarkService` is constructed or how it is loaded.

### 5.4 Strategy

Research papers support three sort orders: by citation count (default), by publication date, or by page count. The sort strategy is passed as a `Comparator<ResearchPaper>`:

```java
Comparator<ResearchPaper> comparator = switch (choice) {
    case "2" -> Comparator.comparing(ResearchPaper::getDatePublished,
                    Comparator.nullsLast(Comparator.naturalOrder()));
    case "3" -> Comparator.comparingInt(ResearchPaper::getPages).reversed();
    default  -> Comparator.comparingInt(ResearchPaper::getCitations).reversed();
};
services.getResearchService().printAllPapersSorted(comparator);
```

The `ResearchPaper` class also implements `Comparable<ResearchPaper>` with citation-descending as the natural order.

---

## 6. Business Rules

| Rule | Enforcement point |
|---|---|
| Max credits: 21 | `Student.addCourse()` -- throws `CreditLimitExceededException` |
| Max course failures: 3 | `MarkService.setMark()` -- throws `TooManyFailsException` |
| ATT1+ATT2 >= 30 to enter final | `Mark.setFinalExam()` -- throws `IllegalStateException` |
| Final >= 20, total >= 50 to pass | `Mark.isPassed()` |
| Supervisor h-index >= 3 | `Student.assignResearchSupervisor()` -- throws `LowHIndexException` |
| Supervisor: Year 4 only | `Student.assignResearchSupervisor()` -- throws `IllegalStateException` |
| Teacher rating: 1-5 | `Teacher.addRating()` -- throws `IllegalArgumentException` |
| One rating per teacher per student | `Student.ratedTeachers` set |

---

## 7. Persistence

All domain objects implement `Serializable`. Each service writes its data to a `.day` file in the `data/` directory. `Services.saveAll()` is called after every state-changing user action.

Backward-compatible deserialization is achieved with `readObject()` in `Course` and `Student`. When an old serialized file is loaded that lacks newer fields, those fields are initialized to safe defaults instead of remaining `null`.

```
data/
  users.dat
  courses.dat
  marks.dat
  research.dat
  news.dat
  messages.dat
  requests.dat
  logs.dat
```

---

## 8. Role Capabilities Summary

| Role | Key actions |
|---|---|
| Admin | Create users of any role, grant/revoke researcher status, view system logs, view all users |
| Student | Register for courses, view marks, view schedule, rate teachers, research block (when Admin grants researcher status: publish papers, join/create projects, update h-index), choose supervisor (Year 4) |
| Teacher | Record marks, view course statistics, research block (publish papers with co-author linking, projects) |
| Manager | Manage course catalog with lessons, approve registrations, assign teachers, process requests/complaints, manage news, generate reports |
| Dean | Process complaints and general requests (approve/reject with comment) |
| Rector | Same as Dean -- both extend Signatory |
| Employee/Researcher | Publish papers with co-author linking, view my/all projects, create/join projects, update h-index |

---

## 9. Problems and Solutions

**Problem:** `Mark` constructor threw exception when setting ATT1 or ATT2 below threshold, because `setFinalExam(0)` unconditionally checked the attestation sum.

**Solution:** Added `score > 0 &&` guard. The check only fires when a non-zero final exam score is being set. Teachers can now record partial attestation marks before the final.

---

**Problem:** `MarkService.failCounts` map was incremented before `TooManyFailsException` was thrown, leaving the map in an inconsistent state.

**Solution:** Compute the new fail count first, check the limit, then write to the map. The map stays unchanged if the exception fires.

```java
int fails = failCounts.getOrDefault(studentUsername, 0) + 1;
if (fails > MAX_COURSE_FAILURES)
    throw new TooManyFailsException(studentUsername, fails, MAX_COURSE_FAILURES);
failCounts.put(studentUsername, fails);
marks.put(key, mark);
```

---

**Problem:** `Course.lessons` and `Student.ratedTeachers` were added after the initial serialized files were created. Deserialization of old files threw `NullPointerException`.

**Solution:** Added `readObject()` migration methods that initialize missing fields to empty collections.

---

**Problem:** `Lesson` existed as a model class but no UI allowed creating lessons, so student schedule view always showed empty.

**Solution:** Added a "Manage lessons" sub-menu in `Manager.editCourse()`. Manager can add lessons (type, room, day, start/end time) or remove them by index. Student schedule now shows real data.

---

**Problem:** Request processing showed UUID strings which users had to copy/paste.

**Solution:** Changed both `Manager` and `Signatory` to show a numbered list of pending requests. User enters a number; the code resolves it to the UUID internally.

---

**Problem:** Non-professor teachers and bachelor students had no way to become researchers through the UI. The research block in `Employee` was guarded by `isResearcher()`, so only users with an existing profile could enter it -- a circular dependency. Students had no research menu at all.

**Solution:** Researcher status is granted and revoked exclusively by `Admin` via a new "g - Grant/revoke researcher status" option in user management. Admin supplies an h-index and school; `Admin.grantRevokeResearcher()` sets the `ResearchProfile` on any `IResearcher` user (Student, non-professor Teacher, Employee). To share the UI across both hierarchies, `manageResearch()` and six helpers were moved from `Employee` to `User`, where they access the interface via `(IResearcher) this`. `Student` now shows option `8 - Research block` only when `isResearcher()` is true; `Teacher` shows option `4` with the same gate. No code is duplicated.

---

## 10. What Is Not Implemented

- Graphical or web interface (console only)
- Automatic schedule conflict detection (room or time overlap)
- Email notifications (simulated as in-system messages)
- Multi-session concurrency (single-user sequential access)
- Full transcript PDF export (formatted text output only)