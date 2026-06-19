# Changelog

All notable changes to the **Lombok To Builder** plugin are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.1] - 2026-06-19

### Fixed
- **Long, mostly-`null` constructor calls now convert.** The **Minimum values to convert** threshold
  counted the values that survive null-skipping, so a call like `new Foo(0, null, null, null, null)`
  collapsed to a single non-null value and fell below the default threshold of `3` — the very case a
  builder reads best for was silently skipped. The threshold now counts the constructor's arguments,
  before null values are dropped, so such a call converts (to `Foo.builder().a(0).build()`). A call
  whose arguments are *all* null is still left alone, since the result would be an empty
  `Foo.builder().build()` that adds nothing over `new Foo()`.

## [0.5.0] - 2026-06-13

### Added
- **Hand-written constructors are left alone by default.** A `new Foo(...)` that resolves to a
  hand-written constructor is no longer folded into a builder, because such a constructor often runs
  additional logic that the builder would bypass. The constructor Lombok generates from a class-level
  `@Builder` is synthetic and a constructor annotated with `@Builder` directly is the builder's own
  source, so both stay convertible. New setting **Convert calls to hand-written constructors**
  (default **off**) under **Settings → Tools → Lombok To Builder** opts back in. Applies to the
  constructor path, the constructor part of a setter chain, the intentions, and the batch action.

  The synthetic-vs-physical distinction relies on JetBrains' **Lombok** plugin being installed; without
  it every constructor appears hand-written.

## [0.4.0] - 2026-06-10

### Added
- **Returned locals are inlined** as a final step of any conversion. When a freshly-built local is
  returned on the very next line, the builder is folded into the `return` and the throwaway variable
  is dropped:
  ```java
  // before
  User u = User.builder().id(1L).name("Ada").build();
  return u;
  // after
  return User.builder().id(1L).name("Ada").build();
  ```
  It fires only on a plain `return u;` immediately following the declaration, so a deferred
  self-referencing setter, a non-trivial return expression (`return u.copy();`), or an intervening
  comment leaves the local in place. New setting **Inline a converted local that is returned on the
  next line** (default on) under **Settings → Tools → Lombok To Builder**. Applies to both the
  constructor and setter paths, the intentions, and the batch action.

## [0.3.0] - 2026-06-09

### Added
- **Self-referencing setters are handled** when folding a setter block. A setter whose argument
  references the variable being built (e.g. a child that points back at its parent —
  `parent.setChild(new Child(parent))`) can't go inside the builder, since that would read the
  variable before it's assigned. It's now kept as a trailing setter right after the builder:
  ```java
  Parent parent = Parent.builder().name("root").build();
  parent.setChild(new Child("leaf", parent));
  ```
  The rest of the block still folds normally. New setting **Keep self-referencing setters after the
  builder** (default on) under **Settings → Tools → Lombok To Builder**; turn it off to leave any
  block containing such a setter entirely unconverted. Applies to the setters→builder path only
  (a constructor can't reference the object it creates).

## [0.2.6] - 2026-06-09

### Fixed
- Replaced a deprecated `ReadAction.compute(ThrowableComputable)` call in the batch action (the
  Marketplace verifier flagged it on 2025.2/2025.3). The folder/module/project file walk runs
  directly on the background thread — VFS traversal is thread-safe and needs no read action — so the
  deprecated API is gone. No behavior change.

## [0.2.5] - 2026-06-09

### Added
- A plugin icon (light + dark), shown in Settings → Plugins and on the Marketplace listing.

### Internal
- Wired up detekt static analysis (`./gradlew detekt`) with a project config, and added tests for
  generic `@Builder` types, fully-qualified `new` expressions, and selection-range handling.
- Split `chainToBuilderText` / `collectChain` into smaller helpers. No change to conversion behavior.

## [0.2.4] - 2026-06-09

### Fixed
- The batch action (Project view: folder / module / project) no longer freezes the IDE on large
  trees. It now runs in a cancellable background task, enumerates files off the EDT, and applies each
  file in its own short write command so the UI stays responsive (previously the whole batch ran in a
  single EDT write action, freezing the editor for the entire run). The editor path (whole file or
  selection) is unchanged — still a single immediate write command.

## [0.2.3] - 2026-06-09

### Fixed
- Nested `@Builder` classes now keep their outer-class qualifier. Converting `new Outer.Inner(...)`
  (or its setter block) now emits `Outer.Inner.builder()...` instead of dropping the outer class and
  producing an uncompilable `Inner.builder()`. The reference is reproduced as written at the `new`
  site, so a usage that already only needs the simple name is unaffected.

## [0.2.2] - 2026-06-08

### Changed
- **Minimum values to convert** now gates constructor conversions only. Setter blocks are always
  converted regardless of the threshold (you're explicitly migrating setters, so the count of
  values no longer suppresses them).

## [0.2.1] - 2026-06-08

### Fixed
- Setter conversion now uses the backing field name for the builder method, fixing primitive
  `boolean isFoo` fields whose setter is `setFoo` but whose builder method is `isFoo` (previously
  produced an invalid `.foo(...)` call).
- Constructor conversion now requires every parameter to match a field. A hand-written constructor
  whose parameter names differ from the fields (e.g. `category` for field `feeCategory`) is left
  unconverted instead of emitting an invalid `.category(...)` call — including constructor + setter
  blocks, which are left untouched rather than dropping the constructor's values.

## [0.2.0] - 2026-06-08

### Added
- Setting **Skip setting null values** (default on) — drops `.x(null)` calls from the generated
  builder chain.
- Setting **Minimum values to convert** (default 3) — a usage is only converted when at least this
  many non-null values would be set; below the threshold the intention is hidden and the batch
  action skips it.

Both settings live under **Settings → Tools → Lombok To Builder** and apply to the constructor and
setter intentions as well as the batch action.

## [0.1.0] - 2026-06-08

### Added
- **Convert constructor to builder** intention — turns `new Foo(...)` into `Foo.builder()...build()`,
  mapping arguments to builder methods by constructor parameter name.
- **Convert setters to builder** intention — folds a `Foo f = new Foo();` declaration plus its
  trailing `f.setX(...)` calls into a single builder chain.
- **Convert Lombok Usages to Builder** batch action — editor (whole file or selection) and Project
  view (file / folder / module / project, recursively).
- Setting **Generate each builder call on a new line** (default on) for multi-line output.
- Conversions are offered only when the target class is annotated with `@Builder` / `@SuperBuilder`.
- Compatible with IntelliJ IDEA 2024.2 and newer.

[Unreleased]: https://github.com/marekpietrasz/lombok-to-builder-plugin/compare/v0.5.1...HEAD
[0.5.1]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.5.1
[0.5.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.5.0
[0.4.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.4.0
[0.3.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.3.0
[0.2.6]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.6
[0.2.5]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.5
[0.2.4]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.4
[0.2.3]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.3
[0.2.2]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.2
[0.2.1]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.1
[0.2.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.0
[0.1.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.1.0
