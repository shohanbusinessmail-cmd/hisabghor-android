# রিলিজ প্রক্রিয়া (Release Guide)

## ডিবাগ APK (সবচেয়ে সহজ — পরীক্ষামূলক ব্যবহারের জন্য)

প্রতিটি সফল CI রানে `hisabghor-debug-apk` নামের আর্টিফ্যাক্ট তৈরি হয়:
GitHub → রিপোজিটরি → **Actions** → সর্বশেষ সফল রান → **Artifacts** → `hisabghor-debug-apk` ডাউনলোড → ফোনে ইনস্টল (অজানা সোর্স থেকে ইনস্টলের অনুমতি লাগতে পারে)।

স্থানীয়ভাবে: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`।

## সাইন করা রিলিজ APK (প্রোডাকশন)

> ⚠️ সাইনিং কী হারালে ঐ কী দিয়ে সাইন করা অ্যাপের আপডেট আর দেওয়া যাবে না। কী ও পাসওয়ার্ড নিরাপদে একাধিক জায়গায় ব্যাকআপ রাখুন। কী কখনো রিপোজিটরিতে কমিট করবেন না।

### ১) কীস্টোর তৈরি (একবার, নিজের কম্পিউটারে)

```bash
keytool -genkeypair -v -keystore hisabghor-release.jks -alias hisabghor \
  -keyalg RSA -keysize 2048 -validity 10000
```

শক্ত পাসওয়ার্ড দিন। ফাইলটি রিপোজিটরির **বাইরে** রাখুন।

### ২ক) স্থানীয় বিল্ড

রিপোজিটরির রুটে `keystore.properties` ফাইল বানান (এটি `.gitignore`-এ আছে):

```properties
KEYSTORE_FILE=/absolute/path/hisabghor-release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=hisabghor
KEY_PASSWORD=...
```

তারপর: `./gradlew assembleRelease` → সাইন করা APK `app/build/outputs/apk/release/`-এ।

### ২খ) GitHub Actions দিয়ে

রিপোজিটরি সেটিংস → Secrets and variables → Actions-এ যোগ করুন:

- `KEYSTORE_BASE64` — `base64 -w0 hisabghor-release.jks` এর আউটপুট
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

তারপর Actions → **Android Release** → Run workflow, অথবা `v1.0.0`-এর মতো ট্যাগ পুশ করুন (`git tag v1.0.0 && git push origin v1.0.0`) — ট্যাগ বিল্ডে GitHub Release-ও তৈরি হবে।

### সিক্রেট না থাকলে

রিলিজ ওয়ার্কফ্লো তবুও চলে, কিন্তু তৈরি হয় **unsigned** APK (`app-release-unsigned.apk`) — এটি ফোনে ইনস্টল **করা যায় না**; এর উদ্দেশ্য শুধু যাচাই করা যে রিলিজ বিল্ড (R8 মিনিফাই ইত্যাদি) সফল হয়। ইনস্টলযোগ্য বিল্ড পেতে হয় ডিবাগ APK ব্যবহার করুন, নয়তো ওপরের সাইনিং ধাপ অনুসরণ করুন।

## যাচাই

- সাইন যাচাই: `apksigner verify --print-certs app-release.apk` (Android SDK build-tools-এ থাকে)।
- প্রতিটি রিলিজের আগে [TESTING.md](TESTING.md)-এর হাতে-কলমে তালিকাটি অন্তত একবার চালানো সুপারিশ করা হয়।

## সংস্করণ বাড়ানো

`app/build.gradle.kts`-এ `versionCode` (পূর্ণসংখ্যা, প্রতিটি প্রকাশে +১) ও `versionName` বদলান, তারপর নতুন ট্যাগ দিন।
