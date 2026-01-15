# APK 빌드 가이드

## 📦 APK 출력 경로

빌드 후 APK 파일은 다음 경로에 생성됩니다:

### Debug 빌드
```
app\build\outputs\apk\noAnalytics\debug\app-noAnalytics-debug.apk
app\build\outputs\apk\analytics\debug\app-analytics-debug.apk
```

### Release 빌드
```
app\build\outputs\apk\noAnalytics\release\app-noAnalytics-release.apk
app\build\outputs\apk\analytics\release\app-analytics-release.apk
```

## 🔨 빌드 명령어

### Debug APK 빌드
```powershell
# noAnalytics flavor (Google 서비스 없음)
.\gradlew.bat assembleNoAnalyticsDebug

# analytics flavor (Google 서비스 포함)
.\gradlew.bat assembleAnalyticsDebug
```

### Release APK 빌드
```powershell
# noAnalytics flavor
.\gradlew.bat assembleNoAnalyticsRelease

# analytics flavor
.\gradlew.bat assembleAnalyticsRelease
```

### 모든 빌드
```powershell
.\gradlew.bat assembleDebug    # 모든 flavor의 debug 빌드
.\gradlew.bat assembleRelease   # 모든 flavor의 release 빌드
```

## 📍 빠른 확인 방법

빌드 후 다음 명령어로 APK 위치 확인:
```powershell
Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk"
```

## 🎯 16KB Alignment 확인

APK 빌드 후:
```powershell
# 1. APK 빌드
.\gradlew.bat assembleNoAnalyticsDebug

# 2. Alignment 확인
.\check_16kb_alignment.ps1
```
