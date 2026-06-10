# Contributing to FocusFilter

Thanks for your interest. FocusFilter is a solo project but contributions are welcome.

## Ground Rules

- Zero internet permission stays. Do not add any network calls, analytics, or tracking of any kind.
- No third-party data SDKs. Ever.
- Test on API 26+ before submitting a PR.
- Keep the SEA market coverage in `SafelistManager` — don't remove regional payment apps.

## What You Can Contribute

- Bug fixes
- New keywords for `SimpleClassifier` or the keyword safelist seeds
- Improved BERT model (must be ONNX format, INT8 quantized, under 10MB)
- UI improvements
- Translations (the classifier handles multilingual content but UI strings are English only)
- Additional payment apps for `SafelistManager` coverage

## What Will Not Be Merged

- Analytics or crash reporting
- Any feature that requires internet permission
- Anything that stores or transmits notification content off-device
- DND integration (removed intentionally)
- Scheduling features (not planned)

## How to Submit

1. Fork the repo
2. Create a branch: `git checkout -b fix/your-fix-name`
3. Make your changes
4. Confirm it compiles: `./gradlew assembleDebug`
5. Run tests: `./gradlew test`
6. Open a pull request with a clear description of what changed and why

## Code Style

Follow the existing Kotlin conventions in the codebase. MVVM pattern, Room for all persistence, ViewBinding in all fragments.
