# Lombok To Builder

An IntelliJ IDEA plugin (Kotlin) that converts **constructor calls** and **setter blocks** of
Lombok `@Builder` classes into the equivalent builder chain.

```java
// before
User u = new User(1L, "Ada", "ada@example.com", true);

User u = new User();
u.setId(2L);
u.setName("Linus");

// after
User u = User.builder().id(1L).name("Ada").email("ada@example.com").active(true).build();

User u = User.builder().id(2L).name("Linus").build();
```

The conversions are **only offered when the target class is annotated with `@Builder`** (or
`@SuperBuilder`) — on the class, a constructor, or a static factory method. If there's no Lombok
builder, no action appears.

## Features

- **Convert constructor to builder** — caret on a `new Foo(...)` call. Constructor arguments are
  mapped to builder methods by parameter name (this matches Lombok's generated all-args
  constructor).
- **Convert setters to builder** — caret on a `Foo f = new Foo();` declaration or on any of its
  trailing `f.setX(...)` calls. The declaration plus the contiguous setters fold into one chain.
- **Batch conversion** via right-click → *Convert Lombok Usages to Builder*:
  - In the **editor** with **no selection**, the whole file is analyzed.
  - In the **editor** with a **selection**, only that range is converted.
  - In the **Project view**, selecting a **file, folder, module, or the project** converts every
    `.java` file underneath it.

> Tip: install/enable JetBrains' **Lombok** plugin too, so the constructors and builder methods
> Lombok generates resolve in the editor.

## Configuration

**Settings → Tools → Lombok To Builder**:

- **Generate each builder call on a new line** (default: **on**) — produces a chopped, multi-line
  chain (indentation follows your project code style):
  ```java
  User.builder()
          .id(1L)
          .name("Ada")
          .build();
  ```
  Disable it to generate the whole chain on a single line:
  ```java
  User.builder().id(1L).name("Ada").build();
  ```

## Why a plugin (and not SSR / OpenRewrite)?

Structural Search & Replace can't reliably do multi-line, structural edits like folding a
declaration plus N setter statements into one expression. A PSI-based plugin can. If you ever need
a *bulk, headless* migration across a whole repo in CI, [OpenRewrite](https://docs.openrewrite.org)
is the better tool — this plugin is for interactive, in-editor edits.

## Building & running

**Gradle must run on JDK 17–21** (Gradle 8.10.2 can't yet run on JDK 25). The *plugin* is compiled
to JDK 21 bytecode (matching the 2025.1 platform it builds against) via a toolchain that Gradle
auto-provisions if needed. With SDKMAN:

```bash
sdk use java 21.0.8-tem    # or any 17–21 JDK; then run ./gradlew as usual
```

```bash
./gradlew test          # run the test suite
./gradlew runIde        # launch a sandbox IDE with the plugin installed
./gradlew buildPlugin   # produce build/distributions/lombok-to-builder-plugin-*.zip
```

### Try it on the sample project

`./gradlew runIde` launches a sandbox IDE. In it, **File → Open…** the [`sample-project/`](sample-project)
folder — a real Gradle + Lombok project — and run the conversions on `Usage.java`. See
[`sample-project/README.md`](sample-project/README.md).

## Installing & publishing

Install a local build into any IntelliJ IDEA: `./gradlew buildPlugin`, then *Settings → Plugins →
⚙ → Install Plugin from Disk…* and pick the zip in `build/distributions/`.

Publishing to the JetBrains Marketplace is **one command**:

```bash
cp publish.env.example publish.env   # fill in token, signing key paths, passphrase (git-ignored)
./publish.sh                         # builds, tests, signs, and uploads
```

See [PUBLISHING.md](PUBLISHING.md) for the one-time account/token/signing-key setup and the
first-release steps.

## Project layout

```
src/main/kotlin/.../LombokBuilderSupport.kt      detection + text building + chain collection
src/main/kotlin/.../ConstructorToBuilderIntention.kt
src/main/kotlin/.../SettersToBuilderIntention.kt
src/main/kotlin/.../BuilderConversionEngine.kt   batch (file / range) conversion
src/main/kotlin/.../ConvertToBuilderAction.kt    editor + Project-view entry point
src/main/resources/META-INF/plugin.xml
src/test/kotlin/...                              tests (LightJavaCodeInsightFixtureTestCase)
sample-project/                                  full Gradle + Lombok project to try the plugin on
```

## Known limitations

- Constructor arguments are mapped to builder methods **positionally by parameter name**; calls
  with varargs or an argument/parameter count mismatch are skipped for safety.
- The setter fold only gathers setters that **immediately follow** the declaration and are called on
  that variable; an intervening statement stops the chain.
- Batch conversion converts constructor calls **with arguments** (a bare `new Foo()` with no
  following setters is left alone to avoid noisy `Foo.builder().build()` results).

## License

MIT — see [LICENSE](LICENSE).
