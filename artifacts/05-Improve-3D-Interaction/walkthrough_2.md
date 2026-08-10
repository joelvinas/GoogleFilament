What changed

CameraGestureStateMachine.kt — the two-finger pan session is no longer opened once (on the 2nd finger landing) and closed once (when a finger lifts). Instead, every two-finger MOVE event opens and closes its own micro-session:

- POINTER_DOWN (1→2 fingers): ends the orbit session, marks isStrafing = true, but does not call grabBegin — there's no prior centroid yet to anchor a delta against.
- MOVE while strafing: computes the new centroid. If a previous centroid exists, fires grabBegin(prevCentroid, strafe=true) → grabUpdate(newCentroid) → grabEnd() as one atomic triplet, then stores the new centroid as "previous." The first MOVE after the second finger lands only records the centroid — no triplet.
- POINTER_UP (2→1 fingers): no pan session to close (the last MOVE already closed its own); just opens a fresh orbit session at the remaining pointer, as before.

OrbitGestureHandler.kt — removed scaleFiredThisEvent and the scaleFired parameter entirely. scaleDetector.onTouchEvent() and stateMachine.processEvent() both run synchronously inside onTouchEvent(), and the pan grab session is now never left open across frames, so there's no window left for the two to collide.

CameraGestureStateMachineTest.kt — fully rewritten around per-frame triplet counting: verifies POINTER_DOWN alone doesn't fire a begin, the first post-POINTER_DOWN MOVE fires no triplet, subsequent MOVEs each fire an independent begin/update/end triplet, and the 2→1 handoff doesn't emit a spurious grabEnd.

Test run: :app:testDebugUnitTest (JAVA_HOME set to the Android Studio bundled JBR) — full suite BUILD SUCCESSFUL; the state machine suite specifically: tests="9" skipped="0" failures="0" errors="0".

Why (the reasoning trail, for README/commit use)

The original design modeled two-finger pan the same way as one-finger orbit: one grabBegin when the gesture starts, a stream of grabUpdates while it continues, one grabEnd when it stops. That works for orbit because nothing else touches the manipulator during an orbit drag. It breaks for pan because zoom's scroll() call is unconditional and runs on the same frames, mutating camera distance state while the pan session sits open and assumes it still owns the interaction. During a mechanically synchronized pinch, the centroid is nearly stationary (by construction — a symmetric pinch has almost no centroid drift) even though the pinch distance is changing rapidly, so the pan session goes long stretches without a grabUpdate to keep it in sync while scroll() keeps changing distance underneath it. Natural human pinching is never perfectly symmetric, so the centroid jitters just enough to keep re-syncing the session — which is why it looked fixed in casual testing but broke on a rig or a careful two-hand pinch.

The earlier fix — gate grabUpdate off when onScale fired that frame (scaleFired) — treated the symptom (both handlers writing in the same frame) rather than the cause (a session staying open across frames it has no business spanning). It's a sticky, gesture-scoped flag bolted onto a structurally unsound session lifetime: it silences the specific collision pattern that was reproduced, but the pan session was still a stateful thing living across an unbounded number of frames, with no guarantee scroll() couldn't still desync it in some other timing arrangement. That's why it fixed natural pinching (which produces frequent, well-distributed onScale firings to gate around) but not the synchronized case (which produces almost none).

Collapsing the pan session to one MOVE event's lifetime removes the category of bug rather than patching an instance of it: there is no longer any multi-frame window during which a scroll() call can land while a pan session is "open but stale." Each frame, zoom and pan each get their turn on the manipulator, sequentially, and neither leaves state open for the other to invalidate. This is why the scaleFired gating could be deleted outright rather than kept alongside the new design — the thing it was working around no longer exists.

Flags for manual on-device retest (not verified here — no device/emulator in this session)

1. Natural pinch — confirm zoom still feels smooth (should be unaffected; scroll() path is untouched).
2. Single-axis isolation — confirm pure pan (centroid moves, distance constant) and pure zoom (distance changes, centroid constant) don't bleed into each other.
3. Rubber-banded/synchronized pinch — this is the case that broke twice before; it's the one that most needs a physical retest.
4. Jank from per-frame grab churn — grabBegin/grabEnd on Filament's manipulator are lightweight anchor-point stores, not GPU/allocation work, and one-finger orbit already calls grabUpdate every MOVE frame at the same cadence — so I'd expect this to be free, but I haven't measured it on-device and it's worth a frame-timing sanity check, not an assumption.

No git operations were performed, per your note — branching/commits are on you.
