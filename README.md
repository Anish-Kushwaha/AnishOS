# AnishOS

**AnishOS** is a native Android + Wear OS companion ecosystem by Anish Enterprises, initially targeting the Samsung Galaxy Watch4.

> Current state: **0.2.0 integration foundation**. This is an active development build, not yet a production release.

## Repository layout

- `app/`, `lib/` — Next.js web control centre.
- `phone/` — native Android phone companion.
- `wear/` — native Wear OS watch application.
- `supabase/migrations/` — PostgreSQL schema migrations.
- `.github/workflows/` — web CI and Android build pipelines.

## Current implemented functionality

### Wear OS application

- AMOLED-black / neon-blue AnishOS interface.
- Focus timer with start, pause and reset.
- Bluetooth status display and system settings shortcut.
- Wear OS Data Layer message client for companion discovery and pairing requests.
- Listener service to receive phone-side link acceptance.
- Health, AI and system configuration screens with truthful feature availability.

### Android phone companion

- Native Android companion app.
- Wear OS Data Layer listener.
- Receives watch link requests.
- User-confirmed app-link handshake and local link state.
- AMOLED / neon-blue interface.

### Web control centre

- Supabase email/password account creation and sign-in.
- Authenticated account dashboard.
- User-owned watch records.
- Responsive dark interface.

### Backend

- Dedicated Supabase project: `nueripsuvrfhylenqumi`.
- `profiles` and `paired_devices` tables.
- Row Level Security enabled with owner-scoped policies.
- Browser clients use the Supabase publishable key only.

## Android device-link protocol

The phone and watch applications share the same Android application ID and use Google Play services Wear OS Data Layer messages. The phone must have the companion installed and the user must explicitly accept a request.

This is an app-to-app local link handshake. It is **not yet** a complete authenticated account pairing flow: the link is not yet bound to a Supabase user/device credential, does not yet provision a revocable cloud device identity, and does not yet relay notifications or health metrics. Do not treat the local link flag as a security credential.

## Build locally

### Web control centre

Requirements: Node.js 22 and npm.

1. `npm install`
2. Copy `.env.example` to `.env.local`.
3. Configure `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`.
4. Run `npm run dev`.

### Android phone and watch apps

Requirements: JDK 17, Android SDK Platform 35, Build Tools 35.0.0 and Gradle 8.11.1.

```bash
gradle :phone:assembleDebug :wear:assembleDebug
```

Outputs:

- `phone/build/outputs/apk/debug/phone-debug.apk`
- `wear/build/outputs/apk/debug/wear-debug.apk`

GitHub Actions builds both packages and uploads separate artifacts for each run.

## Production readiness status

The project is not yet production-ready. Remaining release blockers include:

1. Bind local device pairing to authenticated Supabase accounts using short-lived, single-use pairing challenges and revocable device credentials.
2. Add durable local storage, offline queueing, retry/backoff, and conflict-safe cloud synchronization.
3. Implement opt-in notification relay with explicit Android notification-listener consent and privacy controls.
4. Integrate Health Services with granular permission, background limits, consent, retention and deletion controls.
5. Implement AI through a server-side provider integration with user-configured provider/limits; never ship provider secrets in APKs.
6. Add automated unit, instrumentation, integration, and device tests.
7. Configure crash reporting, monitoring, backups, operational alerts and incident response.
8. Create a protected release-signing process, versioned signed bundles/APKs, staged rollout and rollback.
9. Test on physical Galaxy Watch4 hardware and supported Android phone/One UI versions.
10. Enable Supabase leaked-password protection and review production Auth, rate-limit and email-confirmation settings.

No health metrics or notifications are collected or relayed by the current build. AI is not connected. The current local link state is a development handshake only.

AnishOS is an application ecosystem. It does not replace or modify Samsung's underlying operating system or launcher.
