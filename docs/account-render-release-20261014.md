# Account UI and render filters — 2026-10-14

## Delivered behavior

- Account surfaces reproduce the supplied Prompt Submission layout: dark background, subtle grid and colored glows, sidebar, profile card, Microsoft/Offline tabs. Generated surfaces are owned by `tools/GenerateAccountSurfaces.java`.
- Player heads use the existing PlayerAvatars path. Offline preview identities are synthetic and do not exercise skin downloads.
- Existing IAS authentication, cancellation and account storage remain authoritative. Reference demo credentials and simulated authentication are not used.
- Render Tweaks / Картины hides framed map artwork and replaces painting artwork with canvas backing. Frame entities and inventories are unchanged.
- Render Tweaks / Голограммы rejects text displays and named invisible armor stands before render preparation. Player names are unaffected.
- Both filters default off and respect the parent module toggle.

## Verification

- Java 25: `gradlew.bat test build sourceBundle` succeeded; 261 tests, zero failures/errors. Tasks were up to date on final verification.
- Reviewed account menu selection/deletion routing and Microsoft cancellation on tab changes and removal.
- Generated offline production-chrome previews for wide/compact/empty/error/scrolled and dialog states in `build/account-20261014-check`.
- Minecraft was not launched. Runtime mixin application, actual login, downloaded heads and animation pacing require an authorized game check.

## Limitations

- Small viewports deliberately use the compact account layout rather than compressing the desktop reference.
- Standard deletion/security dialogs retain shared client chrome.
- Custom server holograms implemented using other entity types are not covered by the two known hologram types.
- This release does not establish a measured improvement for player-heavy scenes or restore a specific FPS target. Hiding artwork is an optional visual tradeoff, not proof that every cause of map-related frame loss has been removed.
