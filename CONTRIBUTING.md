# Contributing to Melora

Thank you for helping improve Melora. Contributions to the app, desktop client, web experience, and documentation are welcome.

## Before you begin

- For a substantial feature or redesign, open a discussion or issue first so the scope can be agreed on.
- For a focused fix or documentation correction, you can submit a pull request directly.
- Check the active project branch and any existing issue before starting work.

## Pull requests

- Keep each pull request focused on one change.
- Use a descriptive branch name, such as `fix/artwork-loading` or `docs/setup-guide`.
- Explain the user impact and implementation in the pull request description.
- Include the checks you ran and note anything you could not verify.
- Address review comments and keep the branch current with its target branch.

## Development and checks

Use the Gradle wrapper from the repository root for Kotlin and Android work. For example, on Windows:

```powershell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:testDevDebugUnitTest
```

Run the relevant build or checks for the module you changed. Follow existing style and architecture, add focused tests for new behavior, and avoid unrelated formatting changes.

For audio changes, distinguish file metadata and decoder behavior from what was verified on physical output hardware. Do not report an advertised device capability as proof of actual output behavior.

## Review and licensing

Project maintainers review contributions for correctness, fit, and maintainability. Melora is licensed under the [GNU General Public License v3.0](LICENSE); contributed code and dependencies must be compatible with that license.

Please follow the [Melora Code of Conduct](CODE_OF_CONDUCT.md) in all project spaces.
