# Changelog

All notable changes to the **Lombok To Builder** plugin are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/marekpietrasz/lombok-to-builder-plugin/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.2.0
[0.1.0]: https://github.com/marekpietrasz/lombok-to-builder-plugin/releases/tag/v0.1.0
