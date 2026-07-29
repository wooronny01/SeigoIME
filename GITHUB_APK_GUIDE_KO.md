# SeigoIME GitHub APK 자동 빌드 안내서

이 패키지를 원본 `wooronny01/SeigoIME` 저장소에 **덮어올리면**, GitHub가 자동으로 다음 작업을 수행합니다.

1. JDK 17과 Gradle 8.1.1 준비
2. Android SDK Platform 34와 Build Tools 34.0.0 확인
3. 전체 `kanji_data.csv` 존재 여부와 크기 검사
4. 단위 테스트와 Android Lint 실행
5. 설치 가능한 Debug APK 생성
6. APK 내부에 일본어 사전이 들어갔는지 검사
7. 선택적으로 Android 14 에뮬레이터에 실제 설치
8. Release 서명 Secrets가 있으면 서명된 Release APK도 생성

---

## 가장 쉬운 설치 방법: GitHub 웹에서 올리기

### 1. 원본 저장소를 엽니다

브라우저에서 다음 저장소를 엽니다.

`https://github.com/wooronny01/SeigoIME`

### 2. 안전 백업 브랜치를 만듭니다

1. 왼쪽 위의 `main` 버튼을 누릅니다.
2. `View all branches`를 누릅니다.
3. `New branch`를 누릅니다.
4. 이름을 `backup-before-skins`로 입력합니다.
5. Source는 `main`으로 두고 생성합니다.

### 3. 업데이트 ZIP을 압축 해제합니다

다운로드한 `SeigoIME_GitHub_APK_AutoBuild.zip`을 PC에서 압축 해제합니다.

### 4. 파일을 저장소에 올립니다

1. 저장소의 `main` 브랜치로 돌아갑니다.
2. `Add file` → `Upload files`를 누릅니다.
3. 압축을 푼 폴더 안의 파일과 폴더를 끌어놓습니다.
4. 기존 파일 교체 메시지가 나오면 교체합니다.
5. Commit message에 `Add four skins and APK auto build`라고 적습니다.
6. `Commit changes`를 누릅니다.

> GitHub 웹 업로드에서 숨김 폴더 `.github`와 `.devcontainer`가 보이지 않으면 Windows 탐색기에서 **보기 → 표시 → 숨긴 항목**을 켜세요. 가장 확실한 방법은 GitHub Desktop을 사용하는 것입니다.

### 5. 일본어 사전 파일을 확인합니다

저장소에서 아래 파일을 열어 실제 데이터가 있는지 확인합니다.

`app/src/main/assets/kanji_data.csv`

이 업데이트는 원본 사전을 삭제하지 않습니다. Workflow는 사전이 없거나 너무 작으면 빌드를 실패시켜 잘못된 APK가 배포되지 않도록 합니다.

### 6. Actions 권한을 확인합니다

1. 저장소 상단 `Settings`
2. 왼쪽 `Actions` → `General`
3. `Actions permissions`에서 `Allow all actions and reusable workflows` 선택
4. 아래 `Workflow permissions`는 `Read repository contents permission`이면 충분합니다.
5. `Save`

### 7. 버튼 한 번으로 APK를 만듭니다

1. 저장소 상단 `Actions`
2. 왼쪽에서 `SeigoIME APK 자동 만들기`
3. 오른쪽 `Run workflow`
4. Branch는 `main`
5. `Android 14 에뮬레이터에 APK 설치 테스트`를 체크
6. 녹색 `Run workflow` 버튼

### 8. APK를 받습니다

1. 완료된 실행을 누릅니다.
2. 화면 아래 `Artifacts`로 이동합니다.
3. `SeigoIME-APK-숫자`를 누릅니다.
4. ZIP을 풀면 `SeigoIME-v1.1-debug.apk`가 있습니다.
5. Release Secrets를 등록했다면 `SeigoIME-v1.1-release.apk`도 함께 있습니다.

---

## Debug APK와 Release APK 차이

### Debug APK

- 별도 서명 설정 없이 자동 생성됩니다.
- 휴대전화에 직접 설치해 테스트할 수 있습니다.
- 기존 Play Store/기존 Release 앱 위에는 업데이트 설치가 되지 않을 수 있습니다.

### Release APK

- 기존 앱과 같은 JKS 서명키가 필요합니다.
- 기존 설치 앱 위에 업데이트하려면 반드시 예전 APK와 같은 키와 alias를 사용해야 합니다.
- JKS 파일과 비밀번호는 공개 저장소에 올리지 않고 GitHub Secrets에 넣습니다.

---

## Release APK 서명 설정: 가장 쉬운 방법

Windows에서 GitHub CLI를 설치하고 한 번 로그인합니다.

1. `https://cli.github.com`에서 GitHub CLI 설치
2. PowerShell 또는 명령 프롬프트에서 `gh auth login`
3. 이 패키지의 `2_SET_RELEASE_SIGNING.bat`을 더블클릭
4. 기존 JKS 파일 경로, 비밀번호, alias를 입력

스크립트가 다음 Secrets를 자동 등록합니다.

- `SEIGO_KEYSTORE_BASE64`
- `SEIGO_STORE_PASSWORD`
- `SEIGO_KEY_ALIAS`
- `SEIGO_KEY_PASSWORD`

이후 Workflow를 다시 실행하면 Release APK가 함께 생성됩니다.

---

## Windows에서 완전 원클릭 빌드와 다운로드

처음 한 번 GitHub CLI를 설치하고 `gh auth login`을 실행한 뒤:

`1_GITHUB_APK_BUILD.bat`

을 더블클릭합니다. 스크립트가 자동으로:

1. Workflow 실행
2. 완료될 때까지 상태 확인
3. APK Artifact 다운로드
4. `APK_OUTPUT` 폴더 열기

를 수행합니다.

---

## Codespaces 개발환경 만들기

`.devcontainer/devcontainer.json`이 포함되어 있으므로:

1. 저장소의 녹색 `Code` 버튼
2. `Codespaces` 탭
3. `Create codespace on main`
4. 터미널이 열리고 설정이 끝난 후 다음 명령 실행

```bash
bash scripts/codespaces/build_apk.sh
```

생성 위치:

`APK_OUTPUT/SeigoIME-v1.1-debug.apk`

Codespaces는 컴파일과 APK 생성에는 좋지만, 브라우저 안에서 Android Emulator 화면을 직접 쓰는 용도로는 적합하지 않습니다. 실제 설치 스모크 테스트는 GitHub Actions의 에뮬레이터 작업이 담당합니다.

---

## 빌드 실패 시 가장 먼저 확인할 것

### `kanji_data.csv가 너무 작습니다`

원본 파일 `app/src/main/assets/kanji_data.csv`가 빠졌거나 빈 파일입니다. 원본 저장소에서 복원하세요.

### `SDK location not found`

GitHub Actions에서는 Workflow가 SDK를 자동 준비합니다. 로컬 Android Studio에서만 발생했다면 `local.properties`의 `sdk.dir`을 확인하세요.

### `Could not resolve com.android.tools.build:gradle`

네트워크 또는 Maven 접속 문제입니다. Actions에서 `Re-run jobs`를 한 번 실행하세요.

### Release APK가 안 생김

네 가지 `SEIGO_...` Secrets 중 하나가 빠졌습니다. `2_SET_RELEASE_SIGNING.bat`을 다시 실행하세요.

### 기존 앱 위에 설치가 안 됨

서명이 다릅니다. 기존 앱을 만든 동일한 JKS, alias, 비밀번호가 필요합니다. 키를 잃어버렸다면 기존 설치 앱을 제거하고 Debug APK를 새로 설치해야 합니다.

---

## 보안 주의

- JKS 파일을 GitHub 저장소에 올리지 마세요.
- `storePassword`, `keyPassword`를 `build.gradle`에 직접 적지 마세요.
- 공개된 기존 비밀번호는 교체하는 것이 안전합니다.
- `.gitignore`에서 `*.jks`, `*.keystore`, `keystore.properties`를 계속 제외하세요.
