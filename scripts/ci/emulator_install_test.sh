#!/usr/bin/env bash
set -euo pipefail

mkdir -p emulator-results
exec > >(tee emulator-results/install-test.log) 2>&1

APK="$(find artifacts -type f -name '*debug.apk' -print -quit)"
[ -n "$APK" ] || { echo "Debug APK를 찾지 못했습니다."; exit 1; }

PACKAGE="com.e4gate.seigoime"
IME_ID="com.e4gate.seigoime/.SeigoIME"

echo "사용 APK: $APK"
adb wait-for-device
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

adb install -r "$APK"

INSTALLED_PACKAGES="$(adb shell pm list packages | tr -d '\r')"
if ! grep -Fqx "package:$PACKAGE" <<< "$INSTALLED_PACKAGES"; then
  echo "설치 후 패키지를 찾지 못했습니다: $PACKAGE"
  exit 1
fi
echo "패키지 설치: 성공"

# -a가 없으면 이미 활성화된 IME만 표시되므로,
# 활성화 전 설치 확인에는 반드시 -a -s를 사용한다.
ALL_IME_LIST="$(adb shell ime list -a -s | tr -d '\r')"
printf '%s\n' "$ALL_IME_LIST" > emulator-results/ime-list-all.txt
cat emulator-results/ime-list-all.txt

if ! grep -Fqx "$IME_ID" emulator-results/ime-list-all.txt; then
  echo "설치된 전체 IME 목록에서 서비스를 찾지 못했습니다: $IME_ID"
  adb shell dumpsys package "$PACKAGE" > emulator-results/package-dump-on-failure.txt || true
  exit 1
fi
echo "IME 서비스 설치 등록: 성공"

adb shell ime enable "$IME_ID"

ENABLED_IME_LIST="$(adb shell ime list -s | tr -d '\r')"
printf '%s\n' "$ENABLED_IME_LIST" > emulator-results/ime-list-enabled.txt
cat emulator-results/ime-list-enabled.txt

if ! grep -Fqx "$IME_ID" emulator-results/ime-list-enabled.txt; then
  echo "IME 활성화 후 활성 목록에서 찾지 못했습니다: $IME_ID"
  exit 1
fi
echo "IME 활성화: 성공"

adb shell ime set "$IME_ID"
DEFAULT_IME="$(adb shell settings get secure default_input_method | tr -d '\r')"
echo "현재 기본 IME: $DEFAULT_IME"

if [ "$DEFAULT_IME" != "$IME_ID" ]; then
  echo "기본 IME 전환에 실패했습니다."
  exit 1
fi
echo "기본 IME 전환: 성공"

adb shell am force-stop "$PACKAGE" || true
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 || true
sleep 3
adb exec-out screencap -p > emulator-results/SeigoIME-main-screen.png
adb shell dumpsys package "$PACKAGE" > emulator-results/package-dump.txt

cat > emulator-results/RESULT.txt <<EOF
SeigoIME Android 14 설치 스모크 테스트 성공
Package: $PACKAGE
IME: $IME_ID
Default IME: $DEFAULT_IME
APK: $APK
EOF

cat emulator-results/RESULT.txt
