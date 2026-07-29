#!/usr/bin/env bash
set -euo pipefail

mkdir -p emulator-results
exec > >(tee emulator-results/install-test.log) 2>&1

APK="$(find artifacts -type f -name '*debug.apk' -print -quit)"
[ -n "$APK" ] || { echo "Debug APK를 찾지 못했습니다."; exit 1; }

PACKAGE="com.e4gate.seigoime"
IME_ID="com.e4gate.seigoime/.SeigoIME"

contains_exact_line() {
  local expected="$1"
  local file="$2"
  grep -Fqx "$expected" "$file"
}

wait_for_boot() {
  adb wait-for-device
  for attempt in $(seq 1 90); do
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      adb shell input keyevent 82 >/dev/null 2>&1 || true
      return 0
    fi
    echo "Android 재부팅 완료 대기 중... ${attempt}/90"
    sleep 2
  done
  echo "Android 부팅 완료를 기다리지 못했습니다."
  return 1
}

capture_ime_lists() {
  local suffix="$1"

  adb shell ime list -a -s 2>/dev/null |
    tr -d '\r' > "emulator-results/ime-list-all-${suffix}.txt" || true

  adb shell ime list -s 2>/dev/null |
    tr -d '\r' > "emulator-results/ime-list-enabled-${suffix}.txt" || true
}

wait_for_ime_registration() {
  local phase="$1"

  for attempt in $(seq 1 20); do
    capture_ime_lists "${phase}-${attempt}"

    if contains_exact_line "$IME_ID" \
      "emulator-results/ime-list-all-${phase}-${attempt}.txt"; then
      echo "IME 서비스 설치 등록 확인: 성공 (${phase}, ${attempt}회)"
      cp "emulator-results/ime-list-all-${phase}-${attempt}.txt" \
        emulator-results/ime-list-all.txt
      return 0
    fi

    echo "IME 관리자 목록 갱신 대기 중... ${phase} ${attempt}/20"
    sleep 3
  done

  return 1
}

echo "사용 APK: $APK"
adb wait-for-device
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

echo "APK 내부 Manifest 검사"
AAPT2=""
if [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/build-tools/34.0.0/aapt2" ]; then
  AAPT2="$ANDROID_HOME/build-tools/34.0.0/aapt2"
else
  AAPT2="$(find "${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools" \
    -type f -name aapt2 -perm -111 2>/dev/null | sort -V | tail -n 1)"
fi

if [ -n "$AAPT2" ] && [ -x "$AAPT2" ]; then
  "$AAPT2" dump xmltree \
    --file AndroidManifest.xml \
    "$APK" \
    > emulator-results/apk-manifest.txt
  grep -nE \
    'SeigoIME|android.view.InputMethod|BIND_INPUT_METHOD|android.view.im' \
    emulator-results/apk-manifest.txt || true
else
  echo "aapt2를 찾지 못해 APK Manifest 덤프는 생략합니다."
fi

adb install -r "$APK"

INSTALLED_PACKAGES="$(adb shell pm list packages | tr -d '\r')"
printf '%s\n' "$INSTALLED_PACKAGES" > emulator-results/packages.txt
if ! contains_exact_line "package:$PACKAGE" emulator-results/packages.txt; then
  echo "설치 후 패키지를 찾지 못했습니다: $PACKAGE"
  exit 1
fi
echo "패키지 설치: 성공"

echo "PackageManager의 InputMethod 서비스 검색"
adb shell cmd package query-services \
  --brief \
  --components \
  -a android.view.InputMethod 2>/dev/null |
  tr -d '\r' > emulator-results/query-input-method-services.txt || true
cat emulator-results/query-input-method-services.txt

if grep -Fq "$PACKAGE" emulator-results/query-input-method-services.txt; then
  echo "PackageManager 서비스 선언 확인: 성공"
else
  echo "PackageManager 검색 결과에는 아직 SeigoIME가 없습니다."
fi

# adb install 직후 PackageManager 방송과 InputMethodManager의 목록 재구축은
# 비동기로 처리될 수 있으므로 즉시 실패하지 않고 기다린다.
if ! wait_for_ime_registration "after-install"; then
  echo "설치 직후 목록 갱신이 되지 않아 에뮬레이터를 한 번 재부팅합니다."
  adb reboot
  wait_for_boot

  if ! wait_for_ime_registration "after-reboot"; then
    echo "재부팅 후에도 IME 서비스가 등록되지 않았습니다."

    adb shell dumpsys package "$PACKAGE" \
      > emulator-results/package-dump-on-failure.txt || true
    adb shell dumpsys input_method \
      > emulator-results/input-method-dump-on-failure.txt || true
    adb logcat -d -v brief |
      grep -Ei \
        'InputMethodManager|InputMethodInfo|SeigoIME|PackageManager' \
      > emulator-results/logcat-ime-on-failure.txt || true

    exit 1
  fi
fi

cat emulator-results/ime-list-all.txt

adb shell ime enable "$IME_ID"

for attempt in $(seq 1 20); do
  adb shell ime list -s 2>/dev/null |
    tr -d '\r' > emulator-results/ime-list-enabled.txt || true

  if contains_exact_line "$IME_ID" emulator-results/ime-list-enabled.txt; then
    echo "IME 활성화: 성공"
    break
  fi

  if [ "$attempt" -eq 20 ]; then
    echo "IME 활성화 후 활성 목록에서 찾지 못했습니다: $IME_ID"
    exit 1
  fi

  echo "IME 활성화 반영 대기 중... ${attempt}/20"
  sleep 2
done

cat emulator-results/ime-list-enabled.txt

adb shell ime set "$IME_ID"

DEFAULT_IME=""
for attempt in $(seq 1 20); do
  DEFAULT_IME="$(
    adb shell settings get secure default_input_method 2>/dev/null |
      tr -d '\r'
  )"

  if [ "$DEFAULT_IME" = "$IME_ID" ]; then
    break
  fi

  echo "기본 IME 전환 반영 대기 중... ${attempt}/20"
  sleep 2
done

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
adb shell dumpsys input_method > emulator-results/input-method-dump.txt

cat > emulator-results/RESULT.txt <<EOF
SeigoIME Android 14 설치 스모크 테스트 성공
Package: $PACKAGE
IME: $IME_ID
Default IME: $DEFAULT_IME
APK: $APK
EOF

cat emulator-results/RESULT.txt
