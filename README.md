# MeroShare Bulk — IPO Result Checker

A modern Android app to bulk check IPO allotment results across multiple BOID accounts using the CDSC Nepal API.

---

## Features

- **Bulk Result Check** — Check IPO results for all saved accounts in one tap
- **Live Progress** — See real-time progress bar as each account is checked
- **Summary Stats** — Allotted / Not Allotted / Errors + total share count
- **Account Management** — Add, edit, delete accounts with optional CRN & PIN
- **Foreign Employment Support** — Toggle per account; auto-skips mismatched IPO types
- **AES-256-GCM Encryption** — Transaction PINs encrypted via Android Keystore
- **Company Search** — Filter IPO companies by name or scrip code
- **Dark Navy + Gold UI** — Clean, financial-grade Jetpack Compose interface

---

## Tech Stack

| Layer       | Library                          |
|-------------|----------------------------------|
| UI          | Jetpack Compose + Material 3     |
| Navigation  | Navigation Compose               |
| DI          | Hilt                             |
| Database    | Room (SQLite)                    |
| Network     | Retrofit 2 + OkHttp 4 + Gson    |
| Encryption  | Android Keystore (AES-256-GCM)  |
| Async       | Kotlin Coroutines + Flow         |

---

## Setup

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 26+ (minSdk)
- Internet permission (already in manifest)

### Steps

1. **Clone / extract** the project folder
2. **Open** in Android Studio → `File > Open > meroshare/`
3. Wait for Gradle sync to complete
4. **Run** on an emulator or physical device (API 26+)

---

## Project Structure

```
app/src/main/java/com/meroshare/
├── MeroShareApp.kt              # Hilt application class
├── MainActivity.kt              # Bottom nav host
├── data/
│   ├── model/Models.kt          # Account entity, API DTOs, ResultStatus
│   ├── db/Database.kt           # Room database + DAO
│   └── repository/              # Single source of truth
├── network/ApiService.kt        # Retrofit interface (hardcoded captcha)
├── di/AppModule.kt              # Hilt modules
├── util/EncryptionUtil.kt       # AES-GCM via Android Keystore
└── ui/
    ├── theme/Theme.kt           # Dark navy/gold color scheme
    ├── components/Components.kt # Shared composables
    ├── accounts/                # Account CRUD screen + ViewModel
    └── results/                 # Bulk result check screen + ViewModel
```

---

## API Details

### Fetch Available IPOs
```
GET https://iporesult.cdsc.com.np/result/companyShares/fileUploaded
```

### Check Result
```
POST https://iporesult.cdsc.com.np/result/result/check

{
  "companyShareId": 259,
  "boid": "1301090003335693",
  "userCaptcha": "28157",
  "captchaIdentifier": "b12025e7-12bc-4c87-8919-54b68c03f780"
}
```

> **Note:** The captcha values are hardcoded and bypass the captcha requirement.

---

## Adding Accounts

| Field            | Required | Encrypted | Notes                        |
|------------------|----------|-----------|------------------------------|
| Name / Label     | ✅       | No        | Friendly display name        |
| BOID             | ✅       | No        | Must be exactly 16 digits    |
| CRN              | ❌       | No        | Optional                     |
| Transaction PIN  | ❌       | ✅ Yes    | AES-256-GCM via Keystore     |
| Foreign Employment | —      | No        | Toggle, defaults to off      |

---

## Foreign Employment Logic

- If account has **FE = ON** and selected IPO is **not** a Foreign Employment IPO → **Skipped**
- If account has **FE = OFF** and selected IPO **is** a Foreign Employment IPO → **Skipped**
- Otherwise → **Result is checked normally**

Detection is based on whether the company name contains "Foreign Employment".

---

## Upcoming (v2)

- [ ] Bulk IPO Apply
- [ ] MeroShare login / auth
- [ ] Per-run quantity configuration
- [ ] Push notifications for results
- [ ] Export results to CSV

---

## License

Private use only. Not affiliated with CDSC Nepal or MeroShare.
