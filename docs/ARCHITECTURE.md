# HisabGhor — Architecture

## Overview
HisabGhor (হিসাবঘর) is an offline-first, single-device Android finance app.
All data lives in a local Room (SQLite) database. There is **no network code**
in the app at all: no analytics, no sync, no ads. Money is stored as integer
poisha (`Long`) and every financial event is recorded as a balanced set of
journal postings (double-entry), from which all balances and reports derive.

## Technology choices (verified stable combination, Dec 2024)

| Concern | Choice | Reason |
| --- | --- | --- |
| Language | Kotlin 2.0.21 | Official Android language, Compose compiler plugin |
| Build | Gradle 8.11.1 + AGP 8.7.3, JDK 17 | Stable, supports compileSdk 35 |
| UI | Jetpack Compose + Material 3 (BOM 2024.12.01) | Modern declarative UI, light-only emerald theme |
| Navigation | Navigation Compose 2.8.5 | Single-activity navigation |
| Database | Room 2.6.1 (SQLite) via KSP | Type-safe DAO, migrations, Flow support |
| Async | Coroutines 1.9.0 + Flow | Lifecycle-aware state |
| Background | WorkManager 2.10.0 | Persistent daily reminder checks across reboots |
| Auth | AndroidX Biometric 1.1.0 + Android Keystore | Optional biometrics; PIN verifier protection |
| Prefs | DataStore Preferences 1.1.1 | App settings, security metadata |
| Serialization | kotlinx.serialization 1.7.3 | Versioned JSON backup format |
| minSdk | 26 (Android 8.0) | >95% device coverage in Bangladesh, native `java.time`, adaptive icons |
| target/compileSdk | 35 (Android 15) | Current Play requirement |

One Gradle module (`:app`) with strict package separation keeps the build fast
and the structure clear:

```
com.shohankhan.hisabghor
├── core
│   ├── common        date/time helpers (Asia/Dhaka), result types
│   ├── money         Money (integer poisha), Bengali formatting/parsing, LoanMath
│   ├── model         enums & shared domain models
│   ├── database      Room entities, DAOs, database, migrations
│   ├── accounting    PostingFactory (pure) + ledger balance calculators
│   ├── security      PIN hashing (PBKDF2), Keystore wrapping, lock manager
│   ├── backup        encrypted backup/restore, CSV/JSON/PDF export
│   ├── notifications reminder worker + notification helper
│   ├── designsystem  theme, typography, shared components
│   └── navigation    routes & NavHost
├── data              repositories (single source of truth over Room)
├── feature           one package per feature: dashboard, transactions, accounts,
│                     categories, shops, debts, loans, savings, budgets,
│                     reports, search, settings, lock, more, about
└── di                AppContainer (manual DI)
```

## Layering rules
- UI (Compose) → ViewModel → Repository → Room DAO. UI never touches DAOs.
- Repositories own all writes and run them inside Room transactions.
- `PostingFactory` is pure Kotlin: given a financial event it returns the
  balanced postings. It is unit-tested without Android.
- Balances are **always derived** from postings (`SUM` queries). No mutable
  cached balance is authoritative.
- ViewModels expose `StateFlow<UiState>`; screens are stateless.

## Data integrity strategy
- Every transaction's postings must sum to exactly zero — enforced before
  insert and verified by tests.
- Editing a transaction voids the original and inserts a corrected copy
  (`replacesTransactionId`), inside one DB transaction, with an audit event.
  Reports exclude voided rows, so history is never silently rewritten.
- Deletes of financial records are voids, not hard deletes.
- Restore runs in a single DB transaction: any failure rolls back fully.

## Security model (summary — see docs/SECURITY.md)
- App lock PIN: PBKDF2-HMAC-SHA256 (210k iterations, random salt), verifier
  encrypted with an Android Keystore AES-256-GCM key. Escalating retry delay,
  no permanent lockout. Optional biometric unlock.
- Backups: AES-256-GCM with PBKDF2-derived key from a user-chosen password,
  independent of the PIN (see docs/BACKUP_FORMAT.md).
- `allowBackup=false`, cloud/device-transfer extraction excluded: financial
  data never leaves the device unless the user explicitly exports it.

## Offline & permissions
Required runtime permission: notifications (optional, for reminders).
No INTERNET permission is declared — the app cannot transmit data.
