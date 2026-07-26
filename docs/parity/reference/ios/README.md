# iOS reference captures

These files are frozen simulator evidence for
`ramideltoro/nutsnews-ios@972dda3a0208bd97ddcdc2cd660bbd4360fc6898`.
See [`../../ios-parity-baseline.md`](../../ios-parity-baseline.md) for the
screen/flow/motion inventory, device profiles, source asset checksums, and
reproduction commands.

## Profiles

- Phone: iPhone 16 Pro, iOS 18.6, portrait, 1206 × 2622 pixels.
- Tablet: iPad Pro 11-inch (M4), iPadOS 18.6, portrait,
  1668 × 2420 pixels. The frozen app is an iPhone-only target and therefore
  appears in iPhone compatibility mode on the tablet.

## Evidence

- `iphone-startup.mp4` and `ipad-startup.mp4` record the complete staged splash
  sequence into first-run onboarding.
- `iphone-splash.png`, `iphone-onboarding.png`, and `ipad-onboarding.png`
  preserve startup and first-run layout.
- `iphone-feed.png` and `ipad-feed.png` preserve the returning-user Amber
  dashboard/feed.
- The five `iphone-theme-*-feed.png` files plus `iphone-feed.png` preserve all
  six themes.

The screenshots were captured against the live production feed on July 26,
2026. They are visual references; article text and thumbnails are not
deterministic fixtures. File integrity is locked by `captures.sha256`.
