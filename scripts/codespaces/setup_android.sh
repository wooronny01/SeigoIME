#!/usr/bin/env bash
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_VERSION="15859902"
CMDLINE_ZIP="commandlinetools-linux-${CMDLINE_VERSION}_latest.zip"
CMDLINE_URL="https://dl.google.com/android/repository/${CMDLINE_ZIP}"
CMDLINE_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
GRADLE_VERSION="8.1.1"

sudo apt-get update
sudo apt-get install -y --no-install-recommends curl unzip ca-certificates

mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

curl -fL "$CMDLINE_URL" -o "$TMP_DIR/$CMDLINE_ZIP"
echo "$CMDLINE_SHA256  $TMP_DIR/$CMDLINE_ZIP" | sha256sum -c -
unzip -q "$TMP_DIR/$CMDLINE_ZIP" -d "$TMP_DIR/cmdline"
rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
cp -a "$TMP_DIR/cmdline/cmdline-tools/." "$ANDROID_SDK_ROOT/cmdline-tools/latest/"

export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

if ! command -v gradle >/dev/null 2>&1 || ! gradle --version | grep -q "Gradle $GRADLE_VERSION"; then
  curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    -o "$TMP_DIR/gradle.zip"
  rm -rf "$HOME/.local/gradle-${GRADLE_VERSION}"
  mkdir -p "$HOME/.local"
  unzip -q "$TMP_DIR/gradle.zip" -d "$HOME/.local"
fi

PROFILE_BLOCK=$(cat <<EOF

# SeigoIME Android build environment
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$HOME/.local/gradle-${GRADLE_VERSION}/bin:\$PATH"
EOF
)

if ! grep -q "SeigoIME Android build environment" "$HOME/.bashrc" 2>/dev/null; then
  printf '%s\n' "$PROFILE_BLOCK" >> "$HOME/.bashrc"
fi

export PATH="$HOME/.local/gradle-${GRADLE_VERSION}/bin:$PATH"

java -version
gradle --version | sed -n '1,12p'
sdkmanager --list_installed | sed -n '1,100p'
echo "Codespaces Android 환경 준비 완료"
