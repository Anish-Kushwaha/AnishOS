# AnishOS Phone Companion

Native Android companion app for the AnishOS Wear OS application.

The initial device-link handshake uses the Google Play services Wear OS Data Layer. Both apps must be installed on a compatible Android phone and Wear OS watch, and the user must explicitly accept the link request on the phone.

This first integration milestone establishes local app-to-app messaging only. It does not yet authenticate a Supabase account, relay notifications, upload health data, or provide end-to-end cryptographic identity beyond the platform's app-scoped Data Layer channel. Those are separate production milestones.
