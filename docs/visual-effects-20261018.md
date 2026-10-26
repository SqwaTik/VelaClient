# Visual effects — 2026-10-18

## Changes

- Trajectories submit world-space segments through the existing gizmo collection. Camera movement no longer deforms a previously calculated curve. Impact labels remain separate HUD elements and appear only for airborne projectiles.
- Throwable launch direction follows the resolved Minecraft 26.2 implementation: potion/experience pitch offset affects the vertical component before normalization. Airborne player motion is inherited.
- Held previews refresh at most 120 times per second while moving and 20 while stationary. Flight predictions remain tick-owned, with nine projectiles and 240 simulation steps maximum.
- Dissolve captures the last observed living AvatarRenderState and native model submissions, including equipment surfaces. Native transforms preserve the observed pose. Dissolution lasts 1.1 seconds and emits bounded red sparks; beam and dissolve have local sounds.
- DashTrail ribbon and spiral retain stronger opacity through their middle.
- Near uses camera-relative bearings, cached labels and radial lanes. No angular relaxation changes the direction to a player. Crowded lanes fit within the viewport by reducing their scale.

## Verification

- Java 25: test, build and sourceBundle succeeded; 318 tests, zero failures/errors.
- Regression checks cover launch pitch, inherited jumping motion, gravity/drag order, radial lane wraparound, bearing preservation and lane reuse.
- Dissolve vertex/fragment shaders passed offline shaderc checks for both configured targets.
- No game, launcher or desktop interaction was performed.

## Runtime verification still required

- In-game pose and armor appearance, shader driver behavior, sound balance and dense Near label readability.
- Frame-time improvement is not measured by the build or unit tests.
- Pose capture uses the last living tick snapshot; this is not an exact final rendered-frame capture.
- Dissolve remains a projected HUD mesh, with per-layer face ordering rather than world depth occlusion.
- Prediction cannot confirm terrain or entities that the server has not supplied. Unknown endpoints use the in-flight label.
- Other visual modules in the broader backlog are not part of this release.
