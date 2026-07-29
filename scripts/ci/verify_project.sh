#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "::error::$1"
  exit 1
}

for file in \
  settings.gradle \
  build.gradle \
  app/build.gradle \
  app/src/main/AndroidManifest.xml \
  app/src/main/java/com/e4gate/seigoime/SeigoIME.kt \
  app/src/main/java/com/e4gate/seigoime/JapaneseConverter.kt \
  app/src/main/java/com/e4gate/seigoime/KanjiDatabaseHelper.kt \
  app/src/main/assets/kanji_data.csv; do
  [ -f "$file" ] || fail "필수 파일이 없습니다: $file"
done

CSV="app/src/main/assets/kanji_data.csv"
CSV_BYTES="$(stat -c%s "$CSV")"
CSV_LINES="$(wc -l < "$CSV")"

[ "$CSV_BYTES" -ge 1000000 ] || fail "kanji_data.csv가 너무 작습니다: ${CSV_BYTES} bytes"
[ "$CSV_LINES" -ge 100000 ] || fail "kanji_data.csv 데이터 행이 부족합니다: ${CSV_LINES} lines"

echo "프로젝트 필수 파일: 정상"
echo "일본어 사전 크기: ${CSV_BYTES} bytes"
echo "일본어 사전 행 수: ${CSV_LINES} lines"

if grep -R --line-number -E \
  "storePassword[[:space:]]+['\"][^'\"]+|keyPassword[[:space:]]+['\"][^'\"]+" \
  --include='*.gradle' --include='*.gradle.kts' .; then
  fail "Gradle 파일에 서명 비밀번호가 직접 적혀 있습니다. GitHub Secrets를 사용하세요."
fi

echo "공개 Gradle 파일의 서명 비밀번호 노출 검사: 정상"
