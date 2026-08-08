# Prompt for Gemini — HelloCamera: Grab-State, Rotation, and Gesture Scope Fixes

This is a follow-up pass on the existing `HelloCamera` implementation plan (dual `GestureDetector`/`ScaleGestureDetector`, `Manipulator`-driven orbit/zoom). Please implement the following four changes against the current codebase.

---

## 1. Explicitly terminate the grab state on touch release

**Problem:** Neither `GestureDetector.OnGestureListener` nor `ScaleGestureDetector` exposes a "gesture ended" callback — there's no listener method that fires cleanly on finger-lift. If `manipulator.grabEnd()` isn't called from somewhere explicit, the `Manipulator` can be left permanently in an active "grabbing" state, which causes a visible jump on the next touch.

**Change:**
- In `HelloCameraScreen.kt`, override `onTouchEvent()` directly (separate from, and downstream of, both detectors) and call `manipulator.grabEnd()` on `MotionEvent.ACTION_UP` and `MotionEvent.ACTION_CANCEL`.
- This must be handled against the raw event stream — it does not fall out of either detector automatically.

---

## 2. Rotation: in-place resize, not Activity recreation

**Decision:** Rotation should preserve the user's current camera orbit/zoom position rather than resetting it. Android's default behavior on rotation is to destroy and recreate the Activity, which would tear down and rebuild `Engine`/`Camera`/`Manipulator` from scratch — silently discarding wherever the user had the camera positioned. To avoid that, the Activity should persist across rotation and only the viewport should update.

**Change:**
- Add `android:configChanges="orientation|screenSize|screenLayout"` to the Activity declaration in `AndroidManifest.xml` for the Activity hosting `HelloCameraScreen`.
- Implement a surface-size-changed handler (`SurfaceHolder.Callback.surfaceChanged` or equivalent) that updates **only** the `SwapChain` and the Filament `View`'s viewport/aspect ratio in response to the new dimensions.
- **Critical constraint — do not touch `Engine`, `Camera` position, or `Manipulator` state in this handler.** Rationale to preserve in a code comment: the `Manipulator` holds the user's current orbit position as live in-memory state (eye/target/up vectors). If it gets rebuilt during a rotation-triggered resize, that state is lost and the camera resets to default the instant the device rotates — defeating the entire reason for choosing in-place resize over the default recreate-on-rotate behavior.
- Leave a `// TODO` note that `HelloTriangleScreen`/`MainActivity` should be retrofitted to the same `configChanges` approach in a follow-up pass, once this pattern is validated on-device. Do not implement that retrofit as part of this prompt.

---

## 3. Gesture scope: orbit + zoom only — no panning (explicit design decision, not an omission)

**Context:** `Manipulator.grabBegin(x, y, strafe)` supports two distinct behaviors depending on `strafe`: `false` orbits/rotates the camera around the target, `true` pans/translates it in the local camera plane. Left unspecified, it would be easy for gesture-handling code to accidentally mix the two or leave the behavior ambiguous.

**Change:**
- Single-finger drag maps to orbit only. Always call `manipulator.grabBegin(x, y, false)` — `strafe` is hardcoded `false` for this sample.
- Panning is explicitly **out of scope** for `HelloCamera`. Add a code comment noting this, so it reads as a deliberate scope decision rather than something forgotten. (If panning becomes relevant later, it would be a separate sample/gesture mapping, not a modification of this one.)

---

## 4. Mid-drag rotation: needs a real test, not a unit test

**Why not a unit test:** `GestureDetector`/`ScaleGestureDetector` depend on real `android.view.MotionEvent` objects and Android's touch dispatch pipeline. A plain JVM unit test can't exercise this without mocking away the exact mechanism being verified, which would defeat the point of the test.

**Change — pick one of the following two approaches:**

- **Preferred:** Add an instrumented test (`androidTest`, using Espresso or Compose UI testing APIs) that simulates: touch-down → partial drag → trigger a configuration/orientation change mid-gesture → release. Assert no crash occurs, and that a fresh single-finger drag immediately afterward produces a normal orbit (i.e. `Manipulator` isn't left in a stuck "grabbing" state from the interrupted gesture).
- **Lighter alternative, if full instrumentation isn't worth the investment right now:** Extract the grab-state transitions (`grabBegin` → `grabUpdate`* → `grabEnd`, plus what happens if a resize event arrives between them) into a small, plain Kotlin class with explicit states, and unit-test *that* state machine directly using synthetic call sequences — not real touch events, but the actual state-transition logic that matters.

Either approach is acceptable; state in a code comment which one was chosen and why, so this decision doesn't need to be re-litigated later.

---

## Verification Plan (supersedes/extends the prior plan's verification section)

1. **Camera orbit** — drag one finger; pyramid rotates smoothly.
2. **Camera zoom** — pinch two fingers; zooms correctly; single-finger orbit doesn't jump when fingers are lifted from a pinch.
3. **Grab-state release** — after a drag ends (`ACTION_UP`/`ACTION_CANCEL`), confirm the next fresh touch starts a clean new orbit with no residual jump from the previous gesture.
4. **Rotation, mid-render** — rotate the device while the pyramid is rendering (no active touch); confirm no crash, viewport/aspect ratio updates correctly, and camera orbit/zoom position is unchanged from before rotation.
5. **Rotation, mid-drag** — rotate the device while actively dragging; confirm no crash and that the `Manipulator` recovers cleanly on the next touch (see Section 4 for how this gets tested).
6. **Lifecycle** — navigate to the sample and back 10+ times; confirm 1:1 `[FilamentCamera] Camera Created` / `[FilamentCamera] Camera Destroyed` pairing in Logcat, no leaks.