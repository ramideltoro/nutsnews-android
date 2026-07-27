# T61 secure Android release signing

NutsNews uses a dedicated RSA upload key. Google Play App Signing owns the
eventual app-signing key; this repository and its CI need only the upload key.
The private keystore and passwords must never enter Git, logs, artifacts,
Gradle properties, or configuration-cache state.

The pinned public upload certificate is
`config/signing/nutsnews-upload-certificate.pem`. It is public material and is
used to reject AABs signed by a different key. Its SHA-256 fingerprint is
`82:87:B7:43:78:5D:2B:CD:45:36:43:63:50:E1:0C:F2:B6:F2:7C:D5:9D:7F:35:97:75:5D:D6:CB:73:F7:DC:08`.

## Protected environment contract

The GitHub `release-signing` environment is restricted to protected branches.
It stores exactly these environment secrets:

- `NUTSNEWS_UPLOAD_KEYSTORE_BASE64`
- `NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD`
- `NUTSNEWS_UPLOAD_KEY_ALIAS`
- `NUTSNEWS_UPLOAD_KEY_PASSWORD`

The Security workflow decodes the keystore into a mode-`0600` temporary
directory, builds with `--no-daemon --no-configuration-cache`, verifies the JAR
signature and pinned certificate, uploads only the AAB, and removes the
temporary directory. Pull requests use a random, job-local test key and never
receive protected secrets.

Gradle accepts only the corresponding environment variables, with the decoded
keystore supplied as `NUTSNEWS_UPLOAD_KEYSTORE_PATH`. Any release task fails
before execution if a value or the keystore is missing. The release variant
always selects the dedicated `release` signing configuration; debug signing is
forbidden by the contract validator.

## Generation and backup

Create an upload key on a trusted offline workstation with RSA 4096,
`SHA256withRSA`, a unique `nutsnews-upload` alias, independent random
store/key passwords, and at least 25 years of validity. Export the public
certificate with `keytool -exportcert -rfc`.

Keep three recovery components:

1. the encrypted JKS in two offline encrypted backup locations;
2. both passwords in a password manager or OS secure keychain, separately from
   at least one JKS copy;
3. the public certificate and its SHA-256 fingerprint in source control and
   Google Play Console.

GitHub Environment secrets are deployment inputs, not a backup. Test recovery
at least annually by restoring the JKS and passwords on an isolated workstation,
running `keytool -list`, building an AAB, and verifying it with
`scripts/verify-release-bundle.sh`.

## Rotation and recovery

If an upload credential may be exposed, stop releases, remove or rotate all
four environment secrets, preserve audit logs, and request an upload-key reset
in Google Play Console. Generate a new key using the same contract, register
its public certificate with Play, replace the pinned public certificate and
protected secrets in one reviewed change, then run protected signing before
resuming delivery.

If the key is lost but not compromised, restore the offline JKS and password
records. If recovery fails, use Play Console's upload-key reset process; the
Play app-signing key remains unaffected.

## Validation

Run the credential-free contract locally:

```sh
./scripts/validate-release-signing.sh
```

Run a protected verification through the existing Security workflow:

```sh
gh workflow run security.yml \
  --ref main \
  -f run_protected_signing=true
```

Watch the run at 60-second intervals. A successful `Protected release signing`
job proves `bundleRelease` used the protected upload key and that the resulting
AAB matches the pinned certificate.
