# 16KB 페이지 크기 Alignment 확인 스크립트
# 사용법: .\check_16kb_alignment.ps1

param(
    [string]$ApkPath = "app\build\outputs\apk\noAnalytics\debug\app-noAnalytics-debug.apk"
)

Write-Host "=== 16KB 페이지 크기 Alignment 확인 ===" -ForegroundColor Cyan
Write-Host ""

# APK 파일 존재 확인
if (-not (Test-Path $ApkPath)) {
    Write-Host "오류: APK 파일을 찾을 수 없습니다: $ApkPath" -ForegroundColor Red
    Write-Host "먼저 APK를 빌드하세요: .\gradlew.bat assembleNoAnalyticsDebug" -ForegroundColor Yellow
    exit 1
}

Write-Host "APK 파일: $ApkPath" -ForegroundColor Green
Write-Host ""

# APK를 ZIP으로 추출
$tempDir = Join-Path $env:TEMP "apk_analysis_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    Write-Host "APK 압축 해제 중..." -ForegroundColor Yellow
    Expand-Archive -Path $ApkPath -DestinationPath $tempDir -Force
    
    # lib 폴더 찾기
    $libPath = Join-Path $tempDir "lib"
    if (-not (Test-Path $libPath)) {
        Write-Host "경고: lib 폴더를 찾을 수 없습니다." -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host ""
    Write-Host "=== .so 파일 Alignment 확인 ===" -ForegroundColor Cyan
    Write-Host ""
    
    $hasIssues = $false
    
    # 각 ABI 폴더 확인
    Get-ChildItem -Path $libPath -Directory | ForEach-Object {
        $abi = $_.Name
        Write-Host "ABI: $abi" -ForegroundColor Magenta
        Write-Host ("-" * 50)
        
        Get-ChildItem -Path $_.FullName -Filter "*.so" | ForEach-Object {
            $soFile = $_.FullName
            $fileName = $_.Name
            
            # readelf 또는 objdump로 확인 (NDK 도구 사용)
            $ndkPath = "$env:LOCALAPPDATA\Android\Sdk\ndk\29.0.14206865"
            $readelfPath = Join-Path $ndkPath "toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe"
            
            if (Test-Path $readelfPath) {
                # ELF 헤더에서 LOAD 세그먼트 확인
                $output = & $readelfPath -l $soFile 2>&1
                
                $alignment = "Unknown"
                $is16KB = $false
                
                # LOAD 세그먼트의 Align 값 확인
                $output | ForEach-Object {
                    if ($_ -match "Align\s+(\d+)") {
                        $alignValue = [int]$matches[1]
                        if ($alignValue -ge 16384) {
                            $alignment = "$alignValue bytes (16KB+)"
                            $is16KB = $true
                        } elseif ($alignValue -eq 4096) {
                            $alignment = "$alignValue bytes (4KB)"
                        } else {
                            $alignment = "$alignValue bytes"
                        }
                    }
                }
                
                if ($is16KB) {
                    Write-Host "  ✓ $fileName : $alignment" -ForegroundColor Green
                } else {
                    Write-Host "  ✗ $fileName : $alignment (16KB 미지원)" -ForegroundColor Red
                    $hasIssues = $true
                }
            } else {
                # readelf가 없으면 파일 크기만 확인
                $fileSize = (Get-Item $soFile).Length
                Write-Host "  ? $fileName : Size=$fileSize bytes (readelf 없음, NDK 설치 확인 필요)" -ForegroundColor Yellow
            }
        }
        Write-Host ""
    }
    
    Write-Host "=== 결과 요약 ===" -ForegroundColor Cyan
    if ($hasIssues) {
        Write-Host "경고: 일부 .so 파일이 16KB 정렬되지 않았습니다." -ForegroundColor Red
        Write-Host "NDK r29로 재빌드하거나 라이브러리를 최신 버전으로 업데이트하세요." -ForegroundColor Yellow
    } else {
        Write-Host "모든 .so 파일이 16KB 정렬되어 있습니다!" -ForegroundColor Green
    }
    
} finally {
    # 임시 파일 정리
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "완료!" -ForegroundColor Green
