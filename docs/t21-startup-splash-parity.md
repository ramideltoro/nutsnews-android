# T21 startup splash parity

The Android startup splash follows the staged sequence in the frozen iOS
baseline at commit `972dda3a0208bd97ddcdc2cd660bbd4360fc6898`.

| Elapsed time | Frozen iOS `ContentView` | Android stage |
| ---: | --- | --- |
| 0 ms | Branded gradient; content hidden | Branded gradient; content hidden |
| 500 ms | Fade in chestnut icon | `IconVisible` |
| 1,000 ms | Fade in “NutsNews” | `TitleVisible` |
| 1,500 ms | Fade in “Positive News, Simplified” | `SubtitleVisible` |
| 2,500 ms | Fade out chestnut icon | `IconHidden` |
| 3,000 ms | Fade out “NutsNews” | `TitleHidden` |
| 3,500 ms | Fade out subtitle | `SubtitleHidden` |
| 4,000 ms | Fade splash into app content | `Complete` |

Each element fade uses the iOS duration of 350 ms. The final Android fade and
content scale transition use the iOS handoff duration of 450 ms. Android loads
the same 1254 × 1254 transparent PNG as iOS and uses the same three gradient
colors, 220 dp icon size, type sizes, spacing, and horizontal inset.

The playback owner is an activity-retained `ViewModel`. Ordinary activity
recreation reuses the same instance and current stage instead of replaying the
sequence. A new process starts a new application launch and therefore plays the
sequence from the beginning.

## Recorded Android comparison

Validation used a cold launch on the API 36 `NutsNews_API_36` emulator at
1080 × 2400. A 12-second, 12 Mbps `adb shell screenrecord` capture was sampled
at 250 ms intervals. After the native Android launch surface handed off to
Compose, the sampled frames showed the same relative cues as iOS:

| Cue | iOS target | Android recording |
| --- | ---: | ---: |
| Icon enters | +500 ms | +500 ms |
| Title enters | +1,000 ms | +1,000 ms |
| Subtitle enters | +1,500 ms | +1,500 ms |
| Icon exits | +2,500 ms | +2,500 ms |
| Title exits | +3,000 ms | +3,000 ms |
| Subtitle exits | +3,500 ms | +3,500 ms |

The automated timeline test verifies millisecond-level cue values; the visual
recording comparison is bounded by its 250 ms sampling interval. Rotating the
emulator after the handoff kept the onboarding content visible and did not
replay the splash.
