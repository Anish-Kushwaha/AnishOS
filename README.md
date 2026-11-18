# AnishOS

**AnishOS** is a Wear OS companion ecosystem by Anish Enterprises, initially targeting the Samsung Galaxy Watch4. This repository contains the native watch starter app and the web control-center foundation.

> Current release: **0.1.0 — foundation build**. This is an early working foundation, not a replacement operating system or Samsung system launcher.

## Repository layout

- `app/`, `lib/` — Next.js companion dashboard.
- `wear/` — native Android/Wear OS application (Kotlin).
- `supabase/migrations/` — database schema migrations.
- `.github/workflows/android.yml` — CI build for the Wear OS APK.

## Current functionality

### Wear OS app
- AMOLED-dark AnishOS watch dashboard.
- Focus mode with a working 25-minute countdown, pause and reset.
- Bluetooth status check and shortcut to system Bluetooth settings.
- Health, AI and phone-integration screens with clear status messaging.
- Standalone Wear OS app package targeting API 30+.

### Web control center
- Email/password account creation and sign-in through Supabase Auth.
- Authenticated dashboard.
- Register a Galaxy Watch4 account record.
- View the user's own device records.
- Responsive dark interface.

### Backend and security
- Dedicated Supabase project: `nueripsuvrfhylenqumi`.
- PostgreSQL tables: `profiles`, `paired_devices`.
- Row Level Security enabled on both tables.
- Policies restrict each authenticated user to their own profile and device records.
- Browser uses only the Supabase publishable key. Never place a Supabase secret/service-role key in the browser, watch APK or Git repository.

## Run the web dashboard locally

Requirements: Node.js 20+ and npm.

1. Install dependencies: `npm install`
2. Copy `.env.example` to `.env.local`.
3. Set:
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`
4. Start: `npm run dev`
5. Open `http://localhost:3000`.

The same two variables are configured in the Vercel project for Production, Preview and Development.

## Build the Wear OS APK

Requirements: JDK 17, Android SDK Platform 35 and Gradle 8.11.1.

```bash
gradle :wear:assembleDebug
```

APK output:

```
wear/build/outputs/apk/debug/wear-debug.apk
```

GitHub Actions builds this APK on pushes and pull requests affecting the Wear OS project. Download the artifact from the workflow run.

## Deployment

The `main` branch is connected to the Vercel project `anishos`. A push to `main` triggers a web deployment. Android builds run separately through GitHub Actions.

## Important implementation boundaries

The current watch-registration form creates a database record; it does **not** perform Bluetooth pairing. Live phone synchronization, notification relay, Health Services integration, cloud AI, background sync, production signing and release automation are future milestones. Health data is not collected by this starter build.

AnishOS is an application ecosystem for Wear OS. It does not replace, root, unlock or modify the Galaxy Watch's underlying Samsung/Google operating system.

## Roadmap

1. Stabilize the watch app and CI build.
2. Build the Android phone companion and secure watch pairing flow.
3. Add authenticated device sync through Supabase Edge Functions.
4. Add opt-in Health Services integration with granular permissions.
5. Add the AI assistant through a server-side provider integration.
6. Add production signing, release channels and device testing.
