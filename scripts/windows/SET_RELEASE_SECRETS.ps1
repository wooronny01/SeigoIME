param(
    [string]$Repository = "wooronny01/SeigoIME",
    [string]$KeystorePath = ""
)

$ErrorActionPreference = "Stop"

function Stop-WithMessage([string]$Message) {
    Write-Host ""
    Write-Host $Message -ForegroundColor Red
    Read-Host "Enter를 누르면 종료합니다"
    exit 1
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Stop-WithMessage "GitHub CLI(gh)가 없습니다. 먼저 https://cli.github.com 에서 설치한 뒤 다시 실행하세요."
}

gh auth status 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "GitHub 로그인이 필요합니다." -ForegroundColor Yellow
    gh auth login
}

if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath = Read-Host "기존 앱을 서명한 JKS 파일의 전체 경로"
}
if (-not (Test-Path -LiteralPath $KeystorePath)) {
    Stop-WithMessage "JKS 파일을 찾을 수 없습니다: $KeystorePath"
}

$storePasswordSecure = Read-Host "Keystore 비밀번호" -AsSecureString
$keyAlias = Read-Host "Key alias (예: my-key-alias)"
$keyPasswordSecure = Read-Host "Key 비밀번호" -AsSecureString

$storePassword = [System.Net.NetworkCredential]::new("", $storePasswordSecure).Password
$keyPassword = [System.Net.NetworkCredential]::new("", $keyPasswordSecure).Password
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $KeystorePath)))

Write-Host "GitHub Secrets를 등록합니다..." -ForegroundColor Cyan
$base64 | gh secret set SEIGO_KEYSTORE_BASE64 --repo $Repository
$storePassword | gh secret set SEIGO_STORE_PASSWORD --repo $Repository
$keyAlias | gh secret set SEIGO_KEY_ALIAS --repo $Repository
$keyPassword | gh secret set SEIGO_KEY_PASSWORD --repo $Repository

$storePassword = $null
$keyPassword = $null
$base64 = $null

Write-Host ""
Write-Host "Release 서명 Secrets 등록 완료" -ForegroundColor Green
Write-Host "이제 GitHub Actions를 실행하면 Debug APK와 서명된 Release APK가 함께 생성됩니다."
Read-Host "Enter를 누르면 종료합니다"
