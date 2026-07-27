# Android T52 comparison captures

These captures record the T52 visual-polish result on the API 36
`NutsNews_API_36` phone emulator at 1080 × 2400:

- `api36-phone-onboarding.png` pairs with
  `../ios/iphone-onboarding.png`.
- `api36-phone-amber-feed.png` pairs with `../ios/iphone-feed.png`.

The app was installed from `assembleDebug`, its data was cleared before the
onboarding capture, and onboarding was completed with the frozen defaults
before the feed capture. The feed uses the live production response, so
article text and server-provided categories are not screenshot fixtures.
`captures.sha256` locks the reviewed Android pixels.

Run this from `docs/parity/reference/android` to verify the files:

```shell
shasum -a 256 -c captures.sha256
```
