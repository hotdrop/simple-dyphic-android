# Phase 8 Manual Regression Checklist

## Scope
- Target: Android app (`app/`) only
- Goal: Freeze manual regression scenarios for major user flows before release

## Preconditions
- Build variant: `debug` for manual checks
- Local DB initialized (no crash on first launch)
- Network available for Firebase-related checks
- Health Connect app installed for integration checks

## Scenario A: Calendar -> Record Edit -> Save
1. Launch app and open `Calendar` tab.
2. Tap `Edit selected day`.
3. In `Record` screen, input values in `Breakfast` and `Memo`.
4. Tap `Save`.
5. Verify screen returns to calendar and selected day summary reflects saved values.
6. Tap same day again and verify persisted values are reloaded.

## Scenario B: Record Edit -> Unsaved Back
1. Open `Record` screen from calendar.
2. Change any field.
3. Press back button.
4. Verify discard confirmation dialog is shown.
5. Tap `Keep editing` and verify values remain.
6. Press back again and tap `Discard`; verify return to calendar without saving.

## Scenario C: Settings -> Backup / Restore
1. Open `Settings` tab while signed in.
2. Tap `Backup`; verify operation message is shown and app remains responsive.
3. Tap `Restore`; verify operation message is shown and app remains responsive.
4. Sign out and verify backup/restore items are hidden.

## Scenario D: Health Connect Import
1. Open `Record` screen.
2. Tap `Import from Health Connect`.
3. Verify behavior for each state:
- permission denied: error/guide message shown
- permission granted: step count and kcal are populated
- values already exist and differ: overwrite confirmation dialog shown
4. Confirm overwrite and verify values update.

## Scenario E: Process Reopen / Regression Smoke
1. Force close app and relaunch.
2. Verify last saved record can still be read from calendar summary.
3. Verify Settings tab opens without crash.

## Flutter Asset Cleanup Staging (No Deletion in this phase)
- Keep `flutter_app/` read-only as migration reference.
- Do not add new Dart/Flutter implementation.
- Deletion of obsolete Flutter assets will be handled after Android parity sign-off.
