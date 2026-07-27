# T52 visual and motion parity

This pass compares the native Compose app with
`ramideltoro/nutsnews-ios@972dda3a0208bd97ddcdc2cd660bbd4360fc6898`.
The frozen screen, flow, and motion contracts remain defined in
[`ios-parity-baseline.md`](ios-parity-baseline.md).

## Reviewed captures

| Surface | Frozen iOS reference | API 36 Android result | Review |
| --- | --- | --- | --- |
| Onboarding | [`iphone-onboarding.png`](reference/ios/iphone-onboarding.png) | [`api36-phone-onboarding.png`](reference/android/api36-phone-onboarding.png) | Pass: hierarchy, insets, two-column topics, selected treatments, card radii, gradient, type roles, and scroll reachability have no critical or major gap. |
| Amber feed | [`iphone-feed.png`](reference/ios/iphone-feed.png) | [`api36-phone-amber-feed.png`](reference/android/api36-phone-amber-feed.png) | Pass: header, category rail, dashboard, two-column shortcuts, For You card, borders, gradients, crops, shadows, and spacing have no critical or major gap. |
| Six themes | Frozen iOS theme feed captures in [`reference/ios`](reference/ios) | Approved deterministic API 36 previews in [`../design-system/previews`](../design-system/previews) | Pass: all stored identities, schemes, gradients, accents, text roles, borders, badges, and button treatments remain distinct and matched. |
| Startup | [`iphone-startup.mp4`](reference/ios/iphone-startup.mp4) | T21 API 36 recording documented in [`../t21-startup-splash-parity.md`](../t21-startup-splash-parity.md) | Pass: every staged cue and the final reveal match the frozen timeline. |

The two new Android PNGs are 1080 × 2400 and are integrity-locked by
[`captures.sha256`](reference/android/captures.sha256).

## Motion parity

`NutsNewsMotion` is the single Compose timing contract for M01–M13. This pass
also removed feature-local timing literals so future changes cannot make one
surface drift independently.

| ID | Android parity result |
| --- | --- |
| M01 | The staged splash keeps its 500 ms cues, 350 ms element fades, 1,000 ms hold, and 450 ms handoff. |
| M02 | Destination content crossfades over 250 ms while preserving the 0.99 splash handoff scale. |
| M03 | Every palette color interpolates for 250 ms; the old accent glow morphs toward the new accent for 1,000 ms and resets at 1,050 ms. |
| M04 | Newly composed feed cards enter over 320 ms from 0.22 opacity, 0.96 scale, and +18 dp Y. |
| M05 | Dashboard refresh keeps damping 0.82. |
| M06 | Mood selection keeps damping 0.84. |
| M07 | Read, settings, and original-source actions use radius 22, open after 160 ms, fade for 1,000 ms, and reset at 1,050 ms. |
| M08 | Like feedback uses a 180 ms card glow-in, 1,000 ms active window, 350 ms settle, and 18 particles with 2,000/2,150 ms travel/cleanup. |
| M09 | Unlike removes the transient treatment over 250 ms without particles. |
| M10 | Note/reflection status uses 200/1,800/250 ms enter/hold/exit timing; reflection page glow fades for 900 ms. |
| M11 | Listen Mode uses the frozen 28-bar profile, live speech level/frequency/seed, and 160/180 ms reading/paused transitions. |
| M12 | Share uses a 1,000 ms action glow and keeps the creating treatment for 800 ms after launching the Android Sharesheet. |
| M13 | Like haptics retain the persisted preference gate and 0.85 source intensity contract. |

Decorative motion snaps or is omitted when Android's reduced-motion setting is
active; state, navigation, and confirmation feedback remain available.

## Unavoidable Android-owned differences

No critical or major visual gap remains in the reviewed captures. The
remaining differences are owned by the platform:

- Android renders the status/navigation bars, cutout area, gesture indicator,
  permission prompts, Custom Tabs, Sharesheet, notification UI, TTS engine UI,
  and bottom-sheet mechanics.
- The iOS source uses SF Pro/New York and SF Symbols through system APIs.
  Android uses its native sans/serif families and Material symbols, so glyph
  metrics and icon contours can differ slightly while semantic size, weight,
  alignment, and hierarchy remain matched.
- SwiftUI and Compose use different native text rasterizers and shadow/blur
  compositors. Token values, radii, colors, elevation intent, and animation
  timing are matched even when individual antialiased edge pixels differ.
- Android preserves its 48 dp minimum interactive target where the matching
  iOS painted control is smaller; the extra hit area does not change the
  visible control dimensions.

## Verification

Focused JVM/Compose tests cover the frozen constants, live palette
interpolation, reduced motion, action-open delay, staged like cleanup,
reflection/listen UI, and share reset window. Full repository checks remain
the merge gate, and the API 36 installed APK is used for the final device
smoke pass.
