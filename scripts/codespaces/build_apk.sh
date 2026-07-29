#!/usr/bin/env bash
set -euo pipefail

source "$HOME/.bashrc" || true
bash scripts/ci/verify_project.sh
gradle --no-daemon clean testDebugUnitTest lintDebug assembleDebug --stacktrace
mkdir -p APK_OUTPUT
cp "$(find app/build/outputs/apk/debug -type f -name '*.apk' | head -n 1)" \
  APK_OUTPUT/SeigoIME-v1.1-debug.apk
unzip -l APK_OUTPUT/SeigoIME-v1.1-debug.apk | grep -q 'assets/kanji_data.csv'
sha256sum APK_OUTPUT/SeigoIME-v1.1-debug.apk | tee APK_OUTPUT/SHA256SUMS.txt
ls -lh APK_OUTPUT
