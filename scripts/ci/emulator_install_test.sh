#!/usr/bin/env bash
set -euo pipefail

mkdir -p emulator-results
exec > >(tee emulator-results/install-test.log) 2>&1

APK="$(find artifacts -type f -name '*debug.apk' | head -n 1)"
[ -n "$APK" ] || { echo "Debug APK를 찾지 못했습니다."; exit 1; }

PACKAGE="com.e4gate.seigoime"
IME_ID="com.e4gate.seigoime/.SeigoIME"

echo "사용 APK: $APK"
adb wait-for-device
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

adb install -r "$APK"
adb shell pm list packages | tr -d '\r' | grep -q "package:$PACKAGE"
echo "패키지 설치: 성공"

adb shell ime list -s | tr -d '\r' | tee emulator-results/ime-list.txt
grep -q "$IME_ID" emulator-results/ime-list.txt
echo "IME 서비스 등록: 성공"

adb shell ime enable "$IME_ID"
adb shell ime set "$IME_ID"
DEFAULT_IME="$(adb shell settings get secure default_input_method | tr -d '\r')"
echo "현재 기본 IME: $DEFAULT_IME"
[ "$DEFAULT_IME" = "$IME_ID" ]

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
