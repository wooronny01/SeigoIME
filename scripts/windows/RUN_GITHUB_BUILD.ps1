param(
    [string]$Repository = "wooronny01/SeigoIME",
    [bool]$RunInstallTest = $true
)

$ErrorActionPreference = "Stop"

function Pause-AndExit([string]$Message, [int]$Code = 1) {
    Write-Host ""
    Write-Host $Message -ForegroundColor $(if ($Code -eq 0) { "Green" } else { "Red" })
    Read-Host "Enter를 누르면 종료합니다"
    exit $Code
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Pause-AndExit "GitHub CLI(gh)가 없습니다. https://cli.github.com 에서 설치한 뒤 다시 실행하세요."
}

gh auth status 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "GitHub 로그인을 시작합니다." -ForegroundColor Yellow
    gh auth login
}

$inputValue = if ($RunInstallTest) { "true" } else { "false" }
Write-Host "GitHub APK 빌드를 시작합니다..." -ForegroundColor Cyan
gh workflow run build.yml --repo $Repository -f run_install_test=$inputValue
if ($LASTEXITCODE -ne 0) {
    Pause-AndExit "Workflow 실행 요청에 실패했습니다. build.yml이 main 브랜치에 올라갔는지 확인하세요."
}

Start-Sleep -Seconds 5
$runId = gh run list --repo $Repository --workflow build.yml --limit 1 --json databaseId --jq '.[0].databaseId'
if ([string]::IsNullOrWhiteSpace($runId)) {
    Pause-AndExit "실행 번호를 찾지 못했습니다. GitHub Actions 화면에서 확인하세요."
}

Write-Host "실행 번호: $runId"
gh run watch $runId --repo $Repository --exit-status
if ($LASTEXITCODE -ne 0) {
    gh run view $runId --repo $Repository --log-failed
    Pause-AndExit "빌드가 실패했습니다. 위 오류와 GitHub Actions 로그를 확인하세요."
}

$outDir = Join-Path $PSScriptRoot "..\..\APK_OUTPUT"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
gh run download $runId --repo $Repository --dir $outDir

Write-Host ""
Write-Host "APK 다운로드 완료: $outDir" -ForegroundColor Green
Get-ChildItem -Path $outDir -Recurse -File | Select-Object FullName, Length
Start-Process explorer.exe $outDir
Pause-AndExit "완료되었습니다." 0
