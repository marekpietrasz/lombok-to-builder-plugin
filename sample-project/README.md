# Lombok Builder Sample

A self-contained Gradle + Lombok project for trying the **Lombok To Builder** plugin.

## Open it

1. Run the plugin sandbox from the repo root: `./gradlew runIde` (Gradle on JDK 17–21).
2. In the sandbox IDE: **File → Open…** and select this `sample-project` folder (open it as a
   Gradle project; let the import finish so Lombok is downloaded).
3. The **Lombok** plugin is bundled with IntelliJ IDEA, so `@Builder` / generated `builder()`,
   constructors and setters resolve out of the box. (If types look unresolved: enable
   *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation
   processing*.)

## What to try

Open `src/main/java/com/example/Usage.java`:

| Where | Action |
|---|---|
| Caret on `new User(1L, ...)` | Alt+Enter → **Convert constructor to builder** |
| Caret on the `User u = new User();` block or any `u.setX(...)` line | Alt+Enter → **Convert setters to builder** |
| Right-click editor (no selection = whole file) | **Convert Lombok Usages to Builder** |
| Right-click the `sample-project` folder in the Project view | batch-convert every `.java` underneath |

Run it normally with `./gradlew run` (or the green gutter arrow on `main`) to confirm the converted
code still behaves the same.
