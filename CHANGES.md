# Seigo IME v1.1 변경사항

## 사용자 기능
- 4종 스킨 선택 및 영구 저장
- 앱 런처용 스킨 설정 화면
- 자판 내 빠른 스킨 선택 패널
- 후보어 선택 상태를 테마별 색상으로 표시
- 현재 입력 모드와 Shift 상태 강조
- 터치 눌림 효과와 햅틱 반응
- 가로 화면 전용 축소 치수

## 코드 및 배포
- 테마 팔레트를 `KeyboardSkin` enum으로 중앙 관리
- 하드코딩된 서명 비밀번호 제거
- `keystore.properties`가 있을 때만 Release 서명 적용
- versionCode 2, versionName 1.1

## GitHub APK 자동 빌드 추가

- GitHub Actions 수동 실행 버튼
- Android SDK 34와 Gradle 8.1.1 자동 준비
- 전체 일본어 사전 데이터 검증
- Debug APK 자동 생성
- GitHub Secrets 기반 Release 서명
- Android 14 에뮬레이터 설치 스모크 테스트
- Windows 원클릭 실행/다운로드 스크립트
- Codespaces 재현 가능한 Android 빌드 환경
