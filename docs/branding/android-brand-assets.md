# Android brand assets

The launcher and splash resources come from the frozen iOS baseline
`972dda3a0208bd97ddcdc2cd660bbd4360fc6898`. No generative or replacement
artwork is used.

## Approved sources

| Android resource | Frozen iOS source | Size | SHA-256 |
| --- | --- | --- | --- |
| `drawable-nodpi/brand_icon.png` | `AppIcon-ios-marketing-1024x1024@1x.png` | 1024×1024 | `00a812d26633fd2db1e6941d8e64912dc4de321e32b482329250eb75912b7be2` |
| `drawable-nodpi/brand_splash.png` | `SplashTransparentChestnuts.png` | 1254×1254 | `3ba7557550ccab3720f451cfa8db6c7de3d2eac1d634b4192df395a03bc087f6` |

Both source PNGs are copied byte-for-byte. They are square, use RGBA color,
and retain the transparent corners approved in the iOS artwork.

## Android rendering

- Legacy square and round resource families are deterministic full-composition
  resizes at 48, 72, 96, 144, and 192 pixels. Square variants retain the exact
  iOS framing; round variants apply only the circular clip Android requires.
- API 26 adaptive launchers fit the complete iOS composition into Android's
  centered 66dp safe zone over the source amber background. Launcher-specific
  masks can no longer enlarge or clip the upper-right highlights.
- API 33 themed launchers use a simplified two-chestnut `NN` alpha mark. It is
  only a monochrome mask; the launcher supplies the user's chosen color.
- API 26–30 use the approved 220dp splash mark over the iOS three-stop amber
  gradient.
- API 31+ use the native system splash surface with the approved mark and the
  gradient's center amber, then hand off to `Theme.NutsNews`.
- The approved gradient is intentionally identical in light and dark mode.
  Dark mode uses a darker system-bar brown so status/navigation glyphs remain
  legible without changing the artwork.

Run `./scripts/validate-brand-assets.sh` after any resource change. It checks
the frozen hashes, PNG dimensions and alpha channels, all density sizes, and
the adaptive, round, monochrome, and splash resource wiring.

On macOS, regenerate the density derivatives with:

```shell
swift scripts/generate-brand-assets.swift \
  app/src/main/res/drawable-nodpi/brand_icon.png \
  app/src/main/res
```
