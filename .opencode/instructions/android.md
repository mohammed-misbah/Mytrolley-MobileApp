# MyTrolley Android — OpenCode operating notes

Read `AGENTS.md` in the project root first. It is the authoritative protection
and verification contract. This file only adds the operational details needed
to run builds and tests from an OpenCode shell.

## Environment

- Project root: `C:\Users\user\AndroidStudioProjects\MyTrolley`
- JDK: Android Studio's bundled JBR at `C:\Program Files\Android\Android Studio\jbr`
- Each OpenCode shell call is a fresh process. Set `JAVA_HOME` in the same
  command as Gradle; setting it in an earlier call does not persist.

## Build and test commands (run from the project root)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest
```

Use `/build`, `/test`, and `/verify` for these. Do not substitute another JDK
or different Gradle tasks without approval (AGENTS.md rules 7 and 15).

## Backend and emulator

- The Django backend lives outside this project at
  `C:\Users\user\OneDrive\Documents\MyTrolleyApp\backend`. Never modify it.
- The default base URL is the production catalogue
  `https://products.mytrolley.ae/api/v1/`. For local Django development,
  override with `-PmytrolleyApiUrl=http://10.0.2.2:8000/api/v1/` only when approved.
- After an approved visual change, follow AGENTS.md section 7: build, install,
  launch on the existing emulator, capture a real ADB screenshot, compare it
  with the approved Figma reference, and report the actual screenshot paths.
  Never claim visual completion from compilation alone.

## Project state

- This project is not a Git repository. Do not run `git init` or destructive
  Git commands without explicit approval (AGENTS.md section 9, rule 17).
