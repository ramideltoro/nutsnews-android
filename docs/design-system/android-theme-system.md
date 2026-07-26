# Android theme system

The Compose design system is a direct port of
`NutsNews/Design/NutsNewsTheme.swift` from the frozen iOS baseline
`972dda3a0208bd97ddcdc2cd660bbd4360fc6898`.

## Theme identity

| Android | Stored iOS value | User-facing title | Scheme |
| --- | --- | --- | --- |
| `Amber` | `amber` | Amber | Dark |
| `Sakura` | `sakura` | Sakura | Light |
| `SaaS` | `modernSaaS` | SaaS | Dark |
| `Foxy` | `sanJuan` | Foxy | Light |
| `Friday` | `creativePremium` | Friday | Dark |
| `Bambi` | `moodyCyberpunk` | Bambi | Dark |

`NutsNewsAppTheme.fromStoredValue` also preserves the iOS migration behavior:
`plain` and `dark` become Amber, `darkPink` becomes Foxy, and `lilac` becomes
Sakura. Unknown values fall back to Amber.

## Compose contract

Wrap a feature in `NutsNewsTheme(theme = selectedTheme)`. Feature UI then reads
stable values from:

- `NutsNewsTheme.colors` for accents, text, surfaces, borders, glows, badges,
  liked-card treatments, category dots, and gradient stops.
- `NutsNewsTheme.spacing`, `radii`, `dimensions`, `borders`, and `shadows` for
  the fixed layout and surface scales.
- `NutsNewsTheme.typography` for SwiftUI-equivalent semantic roles and the
  branded serif, rounded-style, metric, button, and label roles.
- `NutsNewsBackground` and `nutsNewsButtonGradient()` for the diagonal
  three-stop background, top-leading radial overlay, and theme button gradient.

The provider also maps each palette into Material 3 so native Compose controls
inherit the correct primary, surface, text, and outline colors. It applies the
theme's forced light/dark system-bar icon treatment rather than following the
device setting. Android's native sans and serif families are used because the
iOS source uses system fonts and contains no distributable font assets.

## Exact structural values

- Spacing: 4, 6, 10, 16, 26, and 42 dp.
- Radii: 6, 10, 16, 26, and 42 dp.
- Card/image/control radii: 26, 16, and 16 dp.
- Feed/detail images: 188 and 210 dp.
- Chip padding: 13 dp horizontal and 8 dp vertical.
- Semantic typography: 34, 28, 22, 20, 17, 17, 16, 15, 13, 12, and 11 sp.

## Deterministic previews

The six approved API 36 renders are in [`previews`](previews). They exercise
the background brushes, typography, surfaces, borders, badges, button
gradients, and core palette swatches. `previews.sha256` locks the reviewed
pixels. The debug-only `ThemePreviewActivity` renders the same
`NutsNewsThemePreview` composable and is not present in release builds.

To reproduce one capture from an installed debug APK:

```shell
adb shell am force-stop com.nutsnews.app
adb shell am start \
  -n com.nutsnews.app/.designsystem.preview.ThemePreviewActivity \
  --es theme modernSaaS
adb exec-out screencap -p > /tmp/saas.png
```

Allow the native Android splash to finish before capturing. During acceptance,
all six previews were captured twice on the same API 36 emulator and every
second image matched its first capture byte-for-byte.

Run `./scripts/validate-theme-previews.sh` after changing a reference image.
Focused JVM tests separately lock every palette value and opacity, metadata and
legacy mapping, structural token, typography role, and category-dot cycle.
