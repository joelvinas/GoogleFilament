What changed

CameraGestureStateMachine.kt — the two-finger pan centroid is now passed through an exponential moving average (EMA) before it drives grabBegin/grabUpdate, instead of being forwarded raw each MOVE frame:

    smoothed = smoothed + alpha * (raw - smoothed)

- prevCentroidX/Y now stores the last *smoothed* centroid (previously it stored the last raw centroid). It doubles as both the grabBegin anchor for this frame's micro-session and the EMA's running value for the next frame.
- The first MOVE after the second finger lands still just seeds prevCentroid at the raw centroid (nothing to smooth against yet) — unchanged from before, no triplet fires that frame.
- hasPrevCentroid is reset to false on POINTER_DOWN (new 2-finger gesture) and in reset() (gesture fully ends), so smoothing state never carries across gestures — a fresh pan always starts by seeding on raw input, not by inheriting lag from whatever the previous pan's smoothed value happened to be.
- alpha is exposed as named companion-object constants — CENTROID_SMOOTHING_ALPHA_LIGHT (0.5), _MEDIUM (0.3), _HEAVY (0.15) — with CENTROID_SMOOTHING_ALPHA pointing at whichever is currently shipped. Swapping strength is a one-line change, no logic digging required.
- Zoom's scroll() path (OrbitGestureHandler's ScaleGestureDetector callback) was not touched at all.

CameraGestureStateMachineTest.kt — the two existing per-frame tests that asserted exact centroid passthrough (testSecondMoveFiresPerFrameGrabTriplet, testThirdMoveFiresAnotherIndependentTriplet, and the compound sequence test) now compute their expected values through the same EMA formula, referencing CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA directly, so they stay correct if the shipped alpha is retuned later. Two new tests were added:

- testCentroidSmoothingConvergesGraduallyTowardStepChange — seeds the centroid at (0,0), steps the raw input straight to (200,200), and asserts the smoothed output lands strictly between 0 and 200 (not equal to either), then moves closer still on a second held frame. Proves the smoothing doesn't just pass raw values through unchanged.
- testCentroidSmoothingResetsBetweenGestures — builds up smoothing lag in one two-finger gesture, ends it, starts a second gesture with new pointer IDs at an unrelated location, and confirms the second gesture's first real triplet is computed from its own seed, not pulled toward the first gesture's trailing value.

Test run: :app:testDebugUnitTest (JAVA_HOME set to the Android Studio bundled JBR) — BUILD SUCCESSFUL. CameraGestureStateMachineTest: tests="11" skipped="0" failures="0" errors="0" (9 existing + 2 new).

Step 1 — what the raw on-device data actually showed

Captured via temporary Log.d instrumentation in OrbitGestureHandler.onTouchEvent(), logging each pointer's raw (x, y) from the MotionEvent alongside the plain-average centroid, for every two-finger MOVE frame. Device: Samsung SM-A366U (Galaxy A36), 450 physical dpi, connected over USB.

Two captures were taken. The first close-fingers attempt only got fingers down to ~114px apart (~0.64cm at this device's density) — that's actually inside the range the user described as already smooth (0.5–1cm), not the "nearly touching" range where the jitter is worst. A second, deliberately tighter capture was taken; even trying hard to touch the fingers together, the digitizer's minimum reported separation was still ~114–136px (~0.65–0.77cm) — the two touch centroids apparently can't get closer than roughly one fingertip-width apart even when the fingers themselves are touching, which is itself a useful data point about the practical floor on "close-fingers" testing on this hardware.

Within that capture, the raw per-pointer coordinates showed real frame-to-frame position noise — direction reversals and non-monotonic jumps of several pixels between consecutive ~8ms-apart MOVE events, on both pointers, independent of the centroid math. Example (times omitted, consecutive frames):

    p1=(525.00,1135.81) p2=(596.00,1024.44)   -> centroid=(560.50,1080.13)
    p1=(525.00,1134.80) p2=(596.00,1021.39)   -> centroid=(560.50,1078.09)
    p1=(525.00,1133.80) p2=(596.00,1017.20)   -> centroid=(560.50,1075.50)

and, a bit further into the same run, a frame where p1 and p2 briefly move in *opposite* y-directions frame-to-frame rather than tracking the same downward trend the surrounding frames show. That answers Step 1's core question directly: the jitter is present in the raw pointer data itself, before our centroid math ever runs. It is not something our averaging introduces or amplifies.

One nuance worth flagging honestly: the original theory specifically expected the two pointers' noise to be *correlated* (moving together), which is what would explain distance/span calculations (zoom) canceling it out while a plain average (pan) does not. A detrended correlation check across the whole capture (subtracting a short moving average from each pointer's raw trace to isolate noise from intended motion, then correlating the residuals) gave a mixed result — moderate correlation on one axis (~0.83 for x), near-zero on the other (~0.05 for y), and the correlation dropped further when restricted to just the close-separation samples. So the "shared noise that cancels in distance but not in averages" mechanism is not cleanly confirmed by this data; it may be a real contributor, or zoom's immunity may owe more to ScaleGestureDetector's own internal smoothing pass on scaleFactor doing most of the work regardless of correlation structure. Either way, the fix doesn't depend on resolving which mechanism dominates: pan's centroid had zero temporal filtering of any kind, so whatever noise exists in the raw stream reached grabUpdate at full strength, while zoom's path already had its own filtering. Smoothing the centroid addresses the actual gap regardless of the precise noise-correlation story.

Step 2 — smoothing approach and tuning

Standard EMA on the centroid, as specified: smoothed = smoothed + alpha * (raw - smoothed), applied only to the value handed to grabBegin/grabUpdate, never to zoom. alpha was implemented as a swappable named constant rather than tuned once and buried, specifically so it could be felt out on-device rather than guessed:

- Tried alpha=0.3 (medium) first: close-fingers pan was still visibly jittery.
- Tried alpha=0.15 (heavy): jitter gone, pan still felt responsive — no perceptible added lag.
- Retested normal-separation pan and pinch-zoom at alpha=0.15: both felt unchanged from before.

CENTROID_SMOOTHING_ALPHA_HEAVY (0.15) is the shipped default. Given the touch stream runs at roughly 100+ Hz (~8-9ms between MOVE events in the captured log), even fairly heavy smoothing converges within a handful of frames — well under the latency a user would consciously notice — which is consistent with 0.15 reading as "smooth, not laggy" in testing.

On-device retest results (this session, same device)

1. Close-fingers pan — smooth, jitter no longer visible, at alpha=0.15.
2. Normal-separation pan — unaffected, still feels responsive.
3. Pinch-zoom — unaffected, as expected (scroll() path untouched).

The temporary raw-pointer Log.d instrumentation added for Step 1 was removed after the finding was confirmed; it is not part of the shipped diff.

No git operations were performed, per instructions — branching/commits are handled manually outside this session.