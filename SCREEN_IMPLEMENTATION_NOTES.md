# DNA hackathon screens

This feature branch adds a prepaid dashboard, a More/services page, and an opt-in Android notification reader in My Account. The existing AI Chat tab is preserved.

## Demo limitations
- Account number, expiry, balance and day-charge estimate are static sample values based on the screenshots.
- Top-up, subscription pause and add-on actions are visual demos only. They do **not** charge money or modify an account.
- The notification reader displays up to 30 *currently active* notifications after the user enables Notification Access in Android Settings. It does not read historical SMS or message databases.
- Notification previews are kept only in process memory; nothing is uploaded or sent to the chatbot. The user should only grant access on a device they control.
- Build and run via Android Studio (or `./gradlew assembleDebug` with an Android SDK), test on a device, and review before merging.

## Merge target

Open a pull request to **ana-says-hi/DNA_hackaton: screens**, not master.
