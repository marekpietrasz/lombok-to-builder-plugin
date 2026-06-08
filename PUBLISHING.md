# Installing & Publishing

## Install on any IntelliJ IDEA (from a local build)

Works on any IntelliJ-based IDE at build 242+ (2024.2 and newer).

1. Build the distributable (Gradle on JDK 17–21):
   ```bash
   ./gradlew buildPlugin
   ```
   The zip lands in `build/distributions/lombok-to-builder-plugin-<version>.zip`.
2. In the IDE: **Settings → Plugins → ⚙ (gear) → Install Plugin from Disk…**, pick the zip.
3. Restart the IDE when prompted.

To widen IDE compatibility, lower `sinceBuild` / raise `untilBuild` in `build.gradle.kts`
(`intellijPlatform.pluginConfiguration.ideaVersion`).

## Publish to the JetBrains Marketplace

### One-time setup

1. **Account & vendor.** Sign in at <https://plugins.jetbrains.com> and accept the developer
   agreement. (Optionally create an organization vendor.)
2. **Marketplace token.** Create a permanent token at
   <https://plugins.jetbrains.com/author/me/tokens>. Export it locally / in CI:
   ```bash
   export PUBLISH_TOKEN=perm:xxxxxxxx
   ```
3. **Signing key** (Marketplace requires signed uploads). Generate a private key + certificate:
   ```bash
   openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
   openssl req -key private_encrypted.pem -new -x509 -days 3650 -out chain.crt
   ```
   Then export (newlines preserved):
   ```bash
   export PRIVATE_KEY="$(cat private_encrypted.pem)"
   export PRIVATE_KEY_PASSWORD="<the passphrase you chose>"
   export CERTIFICATE_CHAIN="$(cat chain.crt)"
   ```
   `build.gradle.kts` already wires these into `intellijPlatform { signing { … } }`.
   **Do not commit the key files** (the repo's `.gitignore` ignores `*.pem`/`*.crt`).

### First version (creates the listing)

The very first upload must go through the web UI so you can pick a name, category, and description:

1. `./gradlew buildPlugin`
2. <https://plugins.jetbrains.com/plugin/add> → upload the zip → choose a category
   (e.g. *Code editing*) → submit. JetBrains reviews new plugins (usually a couple of business days).

### Subsequent versions (automated)

After the listing exists, bump `version` in `build.gradle.kts`, update `<change-notes>` in
`src/main/resources/META-INF/plugin.xml`, then:

```bash
./gradlew verifyPlugin        # run the JetBrains Plugin Verifier first
./gradlew publishPlugin       # signs (if key env vars set) and uploads using PUBLISH_TOKEN
```

`publishPlugin` uploads to the **default** release channel. For pre-releases use a channel:
add `channels = listOf("beta")` under `intellijPlatform.publishing`, and users add the channel's
custom repository URL to install it.

### Handy checks

```bash
./gradlew runIde                          # manual smoke test in a sandbox IDE
./gradlew verifyPluginProjectConfiguration # config sanity
./gradlew verifyPlugin                    # binary compatibility across target IDEs
```
