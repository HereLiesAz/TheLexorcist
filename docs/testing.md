# Testing

## Policy

**Build and test your changes before you commit them.**

This file previously said the opposite:

> **Do Not Test Unprompted:** Do not run any tests without being explicitly
> told to do so... The goal is to maintain development velocity and avoid
> unnecessary builds. Trust in code analysis and targeted reviews to ensure
> quality before resorting to running a full test suite.

`docs/performance.md` reinforced it with a budget of "at most one build, if any
at all." The results were measurable:

- Commit `3b9838c` reached `main` with a version catalog that did not parse.
  Gradle failed during configuration, before compiling anything. It stayed that
  way.
- `utils/DataParser` shipped with every regex wrapped in literal quote
  characters, so date, name and address extraction returned empty on every real
  input for the life of the file. A single run of a single test would have
  caught it.
- `LegalBertService` shipped loading an asset that does not exist in the
  repository, tokenising against a `vocab.txt` whose contents included the
  literal line `... (full vocabulary content) ...`.
- An agent's internal monologue ("Wait, the previous `read_file` output was
  truncated?") was committed into `CaseViewModel.kt`, five lines above a
  function that crashes when a user taps Package.

Code analysis did not catch any of these. Running the code would have.

## What runs

`.github/workflows/ci.yml` runs on every push and pull request:

```
./gradlew projects                                        # catalog + layout
./gradlew :shared:assemble :app:compileDebugKotlin        # compile
./gradlew :shared:allTests :app:testDebugUnitTest         # test
```

It uses no secrets, so it runs on forks. A placeholder `google-services.json`
is generated from `app/google-services.template.json`.

## Running locally

The Google Services plugin needs a `google-services.json` even to compile. For
a local build that does not talk to Firebase, generate a placeholder:

```bash
sed -e 's/{{PROJECT_NUMBER}}/000000000000/' \
    -e 's/{{PROJECT_ID}}/lexorcist-local/' \
    -e 's/{{APP_ID}}/1:000000000000:android:0000000000000000000000/' \
    -e 's/{{CLIENT_ID}}/000000000000-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.apps.googleusercontent.com/' \
    -e 's/{{GOOGLE_SERVICES_API_KEY}}/AIzaSyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/' \
    app/google-services.template.json > app/google-services.json
```

That file is gitignored. Replace it with a real one to exercise Firebase.

Then:

```bash
./gradlew :app:compileDebugKotlin      # Android app compiles
./gradlew :app:testDebugUnitTest       # Android unit tests
./gradlew :shared:allTests             # multiplatform tests (Android + JVM)
./gradlew :shared:build                # every shared target
```

JDK 21 is required. Apple targets in `:shared` are only configured on a macOS
host, so a Linux or Windows build will not attempt them.

## Writing tests

A test must be able to fail for the reason you care about.

`LegalBertServiceTest` is the example to avoid. It mocked `AssetManager.openFd`
so the missing model file could not surface, supplied a six-word substitute for
the placeholder vocabulary, mocked `InterpreterFactory`, stubbed the interpreter
to write `0.5f` into the output array, and then asserted the output contained
`0.5f`. It passed, it reported coverage, and it concealed three shipping
defects while asserting nothing about the code under test. It has been deleted
along with the service.

Practically:

- Prefer testing a pure function over mocking its way to a ViewModel. Anything
  in `:shared/commonMain` is directly testable with no Android framework at all;
  that is a large part of why the module exists.
- When you mock a boundary, ask what a broken implementation behind that mock
  would look like. If the answer is "the test still passes," the mock is doing
  the asserting.
- Assert on outcomes, not on the fact that a call happened. `OcrViewModelTest`
  verifies `processImage` was invoked and has its state assertions commented
  out, so it would pass if the ViewModel discarded the result entirely.
- When you fix a bug, add the test that fails before the fix.
  `DataParserTest`, `DateExtractorTest` and `SpreadsheetDurabilityTest` are all
  written that way, and one of them deliberately pins the old destructive
  write behaviour so it cannot be reintroduced quietly.
