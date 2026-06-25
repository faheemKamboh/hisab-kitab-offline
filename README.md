# Hisab Kitab Offline

Hisab Kitab Offline is a local Android cashbook and settlement ledger app. It is designed for simple shop or personal balance tracking where the main question is: who owes whom, how much, and why?

The app stores data on the device. It does not require a server, account, login, or internet permission.

## Current scope

- Add ledger entries
- Store entries locally with Room/SQLite
- Track running balance
- Dashboard summary
- Ledger list with period filters and search
- Reports summary by period and category
- CSV export through Android's file picker

## Balance rule

The app uses one fixed balance meaning:

- Positive balance: the shop/person owes you
- Negative balance: you owe the shop/person
- Zero balance: settled

Transaction signs:

- Received from Brother: negative impact, because you now hold shop money and owe it back
- Given to Brother: positive impact, because it reduces what you owe
- Expense Paid by Me: positive impact, because the shop/person owes you for it
- Expense from Shop Cash: positive impact, because it reduces the shop cash you are holding
- Settlement Paid: positive impact
- Settlement Received: negative impact
- Adjustment +: positive correction
- Adjustment -: negative correction

## Build from GitHub Actions

This repository includes a workflow that builds a debug APK.

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Select **Build Debug APK**.
4. Run the workflow, or push to `main`.
5. Open the completed run.
6. Download the artifact named `hisab-kitab-debug-apk`.
7. Extract the artifact ZIP and install `app-debug.apk` on your Android phone.

## First phone test

Before using real data, add these test entries and verify the balance:

1. Received from Brother: PKR 50,000
   - Expected balance: -PKR 50,000, meaning you owe the shop/person.
2. Given to Brother: PKR 20,000
   - Expected balance: -PKR 30,000.
3. Expense Paid by Me: PKR 5,000
   - Expected balance: -PKR 25,000.
4. Given to Brother: PKR 25,000
   - Expected balance: PKR 0, settled.

## Data safety

This is an offline app. Local app data can be removed if the app is uninstalled or app storage is cleared. Export CSV before resetting the app or changing phones.

Planned hardening items include PIN lock, encrypted backup/restore, PDF export, receipt attachments, edit history, and signed release builds.

## Development stack

- Kotlin
- Jetpack Compose
- Material 3
- Room/SQLite
- MVVM-style state management
- GitHub Actions for debug APK builds
