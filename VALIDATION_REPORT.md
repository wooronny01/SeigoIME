# 검증 보고서

검증일: 2026-07-29

## 통과 항목

- Android Manifest 및 모든 리소스 XML 문법 검사
- `SeigoIME.kt`에서 참조하는 모든 `R.id`가 자판 XML에 존재
- XML에서 참조하는 모든 style/string/dimen 리소스가 존재
- Kotlin 1.9 컴파일러로 다음 파일의 문법과 타입 흐름 검사
  - `KeyboardSkin.kt`
  - `MainActivity.kt`
  - `SeigoIME.kt`
- Android API 형태의 검사 스텁과 kotlinx-coroutines 클래스패스를 사용한 컴파일 통과
- GitHub Actions YAML 구문 검사
- Kotlin/Gradle 괄호 균형 검사

## 아직 필요한 실제 기기 검사

이 패키지는 원본 저장소에 덮어쓰는 업데이트 패키지입니다. 이 환경에는 Android SDK와
원본의 대용량 일본어 CSV 자산 전체가 없어 실제 APK 빌드 및 휴대전화 설치 검사는
수행하지 못했습니다. 저장소에 업로드한 뒤 포함된 GitHub Actions 또는 Android Studio로
빌드하고 `UPDATE_GUIDE_KO.md`의 체크리스트를 확인하세요.
