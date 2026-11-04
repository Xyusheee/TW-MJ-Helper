# 台牌記帳 — Android / GitHub Actions

Original Traditional Chinese, offline four-player Taiwan-style mahjong ledger.
Independent implementation inspired by the public feature description of 台牌小助手.
No original app assets, branding, or source code are included. No affiliation is implied.

## Status

Source prototype, not a precompiled APK. Pure Java calculation-engine scenario tests are included, but were NOT executed
here because this environment has no Java compiler/runtime.
The Android APK/UI has NOT been compiled or device-tested in the generation environment.
GitHub Actions performs engine tests and then compiles an installable DEBUG APK.
Do not use it for real settlement until your group verifies the results.

## Features

- Four named players, configurable base/rate, discard wins and self-draw.
- Per-payer 1.5x carry, automatic half-debt forgiveness on winner change.
- Dealer bonus 2n+1, per-payer dealer liability, dice toggle, automatic dealer rotation.
- Central penalty pot; full settlement; explicit manual half settlement.
- Exact BigDecimal amounts, no implicit cent rounding.
- Local persistence, recent-operation undo, JSON export/import.
- Meal expense reconciliation, history, monthly/yearly settled net reports.
- Projected totals and simplified transfer suggestions. No actual payment service.
- Rules reminder; not a playable mahjong game or automatic tile/fan recognizer.

## Build using GitHub website (no Android Studio required)

1. Extract this ZIP on a computer. Upload the CONTENTS to a repository, not the ZIP itself.
   Repository root must contain settings.gradle, build.gradle, app/, tests/, and .github/.
   Do not upload the outer folder as an extra directory. Enable hidden-file display so
   .github/workflows/android.yml is included.
2. Create a GitHub repository and upload those files. Commit to the default branch.
   If web upload omits .github, use Add file > Create new file; name it
   .github/workflows/android.yml and paste the workflow from this project.
3. Open Actions. Enable workflows if prompted. The push starts Build Android APK.
4. To run manually: Actions > Build Android APK > Run workflow > select default
   branch > Run workflow. The workflow must be present on the default branch for
   GitHub to expose the manual trigger.
5. Open the new run, then the build job. Engine tests must pass and Build debug APK
   must finish green. On the run summary, download taiwan-mahjong-debug-apk from
   Artifacts. Extract it to obtain app-debug.apk.
6. Transfer APK to Android 8.0+ phone, open it, and permit installation from that
   browser/file-manager when Android prompts. This is a debug build for testing.
   It is not a Play Store release. No signing secrets are needed for this CI build.

### Upload with Git instead (reliably includes .github)

Run from the extracted project directory, replacing YOUR_ACCOUNT and YOUR_REPOSITORY:

```sh
git init
git add .
git commit -m "Add Android mahjong ledger"
git branch -M main
git remote add origin https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
git push -u origin main
```

Use an empty repository for these commands. If adding to an existing repository,
clone it first and copy these files into the clone rather than force-pushing.
GitHub authentication is required; do not put credentials in project files.

## Toolchain

AGP 8.7.3, Gradle 8.9, JDK 17, compile/target SDK 35, min SDK 26.
CI explicitly installs Gradle with setup-gradle and runs `gradle`, NOT `./gradlew`.
This project intentionally has no Gradle wrapper JAR or gradlew files.
Do not add chmod +x gradlew or invoke ./gradlew to this workflow.

Local build with JDK17, Android SDK35 and Gradle8.9 installed:

```sh
gradle :app:assembleDebug --no-daemon --stacktrace
```

APK: app/build/outputs/apk/debug/app-debug.apk
Android SDK path: set ANDROID_HOME, or use an uncommitted local.properties sdk.dir.

## Calculation interpretation (read before testing)

Input fan excludes dealer bonus. Dealer bonus is one combined 2n+1 term, not
1 plus another 2n+1. Initial dealer has n=0. Dealer win increases n; other winner
rotates dealer one seat and resets n. Draws require manual dealer adjustment.

For the current streak winner, only payers liable in this hand multiply their
own old debt by 1.5. Other debts remain untouched. When winner changes, settle old
chain first. If new winner self-draws OR old winner discards to new winner, new
winner pays HALF their own debt to old winner; others pay full. Then new hand
starts a new debt chain. Never halve both old and new winnings.

Dice unchecked suppresses the entire combined bonus on dealer wins. Dealer
liability to another winner still applies. Confirm this interpretation with your
group: your wording may instead preserve the initial +1 even without dice.

Your $36 -> $74 -> $37 monetary example excludes dealer bonuses, whereas your
other rules explicitly require them. Tests isolate that monetary example using
dice unchecked. In normal play East dealer self-draw at 13 input fan with 10/2
and visible dice costs EACH payer $38, not $36. If your quoted 13 already includes
the dealer +1, enter 12 instead.

Default 10/2 follows the usual one-fifth example, not the stated no-agreement
fallback 10/10. Change settings before play as appropriate. No automatic fan
pattern checking: the supplied text does not include a complete fan table.

See app/src/main/assets/rules.txt for the complete implementation interpretation
and table etiquette reminders.

## Acceptance checks on the actual APK

1. Set 10/2, initial dealer East. Dice checked, East self-draw 13 input fan:
   each opponent owes 38. Undo once to reset.
2. Dice unchecked, East self-draw13: each owes36. East wins5 from South:
   South owes74, West/North36. East discards to South at0 fan:
   old-chain settled balances are East+109, South-37, West-36, North-36.
   New South chain also exists, so projected totals differ from settled balances.
3. Fresh ledger, East dealer at streak3, West self-draw10: East owes44,
   South/North30. (10 + 17*2 =44; 10 + 10*2=30.)
4. Fresh ledger, South penalty: South-10 and pot10. Next winner collects10;
   four-player balance plus pot stays zero.
5. Enter meal paid100 by East, share25 each: East+75, others-25.
6. Export backup, close/reopen, verify persistence. Import backup and verify
   names, balances, debts, dealer, pot, and history. Test undo and report totals.
7. Test UI keyboard, navigation, phone rotation, and Android version compatibility.

## Debug builds and distribution

Debug APKs are automatically debug-signed by the build environment. A new CI
runner may use a different debug key, so a later APK may fail to install over an
older build. EXPORT A BACKUP before uninstalling; uninstall loses local data.
For reliable update distribution use a permanent private release keystore and
release-signing workflow. This project does not configure release signing.

## CI troubleshooting

- Queued means build has not started. Do not edit source just because it queues.
- Red run: open failed step and inspect first actual error, not only exit code1.
- Missing gradlew: this supplied workflow uses gradle; check you committed the
  replacement and opened the NEW run, not an old failed run.
- Blank logs: check run Summary/status; download log archive for completed runs.
- Missing Run workflow: check default branch and workflow_dispatch in YAML.
- Missing APK artifact: inspect Build debug APK and Upload APK steps.
- Android plugin/dependency downloads require access to Google/Maven repositories.
- Keep older workflows from building unrelated directories; remove/disable obsolete
  APK workflows if replacing a previous prototype.

## Privacy / limitations

No INTERNET permission, accounts, analytics, ads, or external storage permission.
JSON backups use Android document picker. Backups contain player names and money
records. No cloud backup: keep your own exports. Monthly reports use local device
dates, settled ledger changes only, and include meals/penalties. There is one
continuous ledger; export before clearing for a new session. Not independently
audited accounting software. Verify rules and amounts before real-money use.
