# Seigo IME 스킨 업데이트 v1.1 적용 안내

## 포함된 기능

- 모찌 파스텔, 사쿠라 글라스, 세이고 블루, 말차 나이트 4종 스킨
- 앱 실행 화면에서 스킨 미리보기 및 선택
- 자판 상단 `스킨` 버튼에서 즉시 변경
- 선택한 스킨 자동 저장
- 둥근 키캡과 눌림 효과
- 후보 단어 알약형 버튼과 선택 강조
- 일반키, 기능키, 스페이스, 입력키 색상 구분
- 두벌/단모, 히라가나/가타카나, Shift 상태 강조
- 세로/가로 화면별 자판 높이 조정
- 키 입력 햅틱 피드백
- 앱 버전 1.1 / versionCode 2

## 적용 방법

1. 원본 SeigoIME 저장소를 컴퓨터에 준비합니다.
2. 이 ZIP의 폴더 구조를 유지한 채 원본 저장소 위에 덮어씁니다.
3. 패키지의 `.gitignore`가 서명키와 로컬 설정 파일을 Git에서 제외하는지 확인합니다.
4. `keystore.properties.example`을 `keystore.properties`로 복사합니다.
5. 기존 업데이트 서명에 사용하는 JKS 파일 경로, 비밀번호, 별칭을 입력합니다.
6. Android Studio에서 Gradle Sync 후 테스트합니다.
7. Release APK 또는 AAB를 빌드합니다.

## 매우 중요한 서명 주의사항

기존에 배포한 앱을 업데이트하려면 이전 버전과 동일한 서명키로 서명해야 합니다.
다만 서명키와 비밀번호는 GitHub에 올리면 안 됩니다. 현재 공개 저장소에 노출된
키 또는 비밀번호가 있다면 가능한 배포 방식에 맞춰 키 교체 절차를 검토하세요.

## 테스트 체크리스트

- 앱 실행 후 네 스킨이 모두 보이는지
- 선택한 스킨이 앱 재실행 후 유지되는지
- 자판 상단 `스킨` 버튼이 열리고 네 스킨이 즉시 적용되는지
- 두벌/단모 전환
- 히라가나/가타카나 전환
- 숫자 및 기호 1/2 화면
- 후보어 터치와 스페이스 순환
- 세로/가로 화면
- 카카오톡, 메시지, 브라우저 입력창
- 기존 버전 위에 업데이트 설치 가능한지

## 파일 구성

기존 파일 교체:
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/e4gate/seigoime/SeigoIME.kt`
- `app/src/main/res/layout/keyboard_view.xml`
- `app/src/main/res/values/styles.xml`

새 파일:
- `app/src/main/java/com/e4gate/seigoime/KeyboardSkin.kt`
- `app/src/main/java/com/e4gate/seigoime/MainActivity.kt`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values-land/dimens.xml`
- `app/src/main/res/values/strings.xml`


## GitHub Actions에서 APK 자동 생성

패키지에 포함된 `.github/workflows/build.yml`은 다음처럼 작동합니다.

- 서명 Secrets가 없으면 테스트용 Debug APK 생성
- 네 가지 서명 Secrets가 있으면 업데이트용 Release APK도 생성

저장소의 `Settings → Secrets and variables → Actions`에서 아래 Secrets를 만드세요.

- `SEIGO_KEYSTORE_BASE64`
- `SEIGO_STORE_PASSWORD`
- `SEIGO_KEY_ALIAS`
- `SEIGO_KEY_PASSWORD`

Windows PowerShell에서 JKS 파일을 Base64 문자열로 바꾸는 명령:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks"))
```

출력된 긴 문자열 전체를 `SEIGO_KEYSTORE_BASE64`에 넣습니다.

소스를 `main` 브랜치에 올리면 Actions가 자동으로 실행됩니다.
완료 후 `Actions → 실행된 작업 → Artifacts`에서 APK를 받습니다.

기존 설치본을 덮어쓰는 업데이트 APK는 반드시 이전 버전과 같은 키로
서명된 `SeigoIME-v1.1-release`를 사용해야 합니다. Debug APK는 기존
Release 앱 위에 업데이트 설치할 수 없습니다.

## 공개 저장소에서 기존 키 제거

`.gitignore`를 추가해도 이미 Git에 추적된 JKS 파일은 자동으로 사라지지 않습니다.
로컬에 안전하게 백업한 뒤 다음 명령으로 저장소에서만 제거하세요.

```bash
git rm --cached my-release-key.jks
git add .gitignore app/build.gradle .github/workflows/build.yml
git commit -m "Secure signing configuration"
```

과거 커밋에 키와 비밀번호가 남아 있으므로, 공개 배포 중인 앱이라면
Google Play App Signing 또는 사용 중인 배포 방식의 키 교체 가능 여부도 확인하세요.
