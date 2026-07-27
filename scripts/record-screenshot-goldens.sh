#!/usr/bin/env bash

set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

./gradlew \
  --stacktrace \
  --console=plain \
  -Pnutsnews.recordGoldens=true \
  testDebugUnitTest \
  --tests 'com.nutsnews.app.parity.*GoldenTest'
