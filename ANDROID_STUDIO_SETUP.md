# Android Studio에서 프로젝트 실행 가이드

## ✅ 빌드 상태
```
BUILD SUCCESSFUL in 1m 52s
```

---

## 📱 Android Studio에서 디버깅 모드로 실행하기

### 1단계: 프로젝트 열기

1. **Android Studio 실행**
2. **File → Open** (또는 `Ctrl + O`)
3. 프로젝트 폴더 선택: `C:\Users\Admin\Desktop\Setlone-Wallet`
4. **OK** 클릭
5. Gradle 동기화가 자동으로 시작됩니다 (처음에는 몇 분 걸릴 수 있음)

### 2단계: Build Variant 선택

프로젝트에 **analytics**와 **noAnalytics** 두 가지 빌드 변형이 있습니다:

1. 왼쪽 하단의 **Build Variants** 탭 클릭
   - 또는 **View → Tool Windows → Build Variants**
2. **app** 모듈의 **Active Build Variant** 선택:
   - `analyticsDebug` (기본값, Firebase Analytics 포함)
   - `noAnalyticsDebug` (Analytics 없음)
3. 원하는 변형 선택

### 3단계: 에뮬레이터 설정

#### A. 기존 에뮬레이터 사용
1. 상단 툴바의 **Device Manager** 아이콘 클릭 (또는 `Tools → Device Manager`)
2. 사용 가능한 에뮬레이터 목록에서 원하는 기기 선택
3. **▶ Play** 버튼 클릭하여 에뮬레이터 시작

#### B. 새 에뮬레이터 생성
1. **Device Manager** → **Create Device**
2. 원하는 기기 선택 (예: Pixel 5, Pixel 6 등)
3. **Next** → 시스템 이미지 선택 (API 24 이상 권장)
4. **Next** → 설정 확인 후 **Finish**

### 4단계: 디버깅 모드로 실행

#### 방법 1: Run 버튼 사용 (권장)
1. 상단 툴바에서 **Run** 버튼 클릭 (녹색 재생 아이콘)
   - 또는 `Shift + F10`
2. 에뮬레이터가 실행 중이면 자동으로 앱이 설치되고 실행됩니다
3. 디버깅 모드로 실행됩니다 (브레이크포인트 사용 가능)

#### 방법 2: Debug 버튼 사용
1. 상단 툴바에서 **Debug** 버튼 클릭 (벌레 아이콘)
   - 또는 `Shift + F9`
2. 디버깅 세션이 시작되고 앱이 실행됩니다

#### 방법 3: 터미널에서 실행
```powershell
# 에뮬레이터 목록 확인
.\gradlew.bat -q :app:installAnalyticsDebug

# 또는 noAnalytics 버전
.\gradlew.bat -q :app:installNoAnalyticsDebug
```

### 5단계: 디버깅 기능 사용

#### 브레이크포인트 설정
1. 코드 왼쪽 여백을 클릭하여 빨간 점(브레이크포인트) 설정
2. 앱 실행 중 해당 코드 실행 시 자동으로 멈춤
3. 변수 값 확인, 단계별 실행 등 가능

#### 디버깅 도구
- **Variables**: 현재 스코프의 변수 값 확인
- **Watches**: 특정 변수/표현식 모니터링
- **Call Stack**: 호출 스택 확인
- **Logcat**: 로그 메시지 확인

---

## 🔧 문제 해결

### Gradle 동기화 실패
```
File → Invalidate Caches / Restart → Invalidate and Restart
```

### 에뮬레이터가 보이지 않음
1. **Tools → SDK Manager**
2. **SDK Tools** 탭
3. **Android Emulator** 체크 후 **Apply**

### ADB 연결 오류
```powershell
# ADB 재시작
adb kill-server
adb start-server
adb devices
```

### 빌드 변형이 보이지 않음
1. **File → Sync Project with Gradle Files**
2. 또는 **Build → Rebuild Project**

---

## 📝 참고사항

### 빌드 변형 설명
- **analyticsDebug**: Firebase Analytics 포함, 디버그 모드
- **noAnalyticsDebug**: Analytics 없음, 디버그 모드
- **analyticsRelease**: Analytics 포함, 릴리스 모드 (서명 필요)
- **noAnalyticsRelease**: Analytics 없음, 릴리스 모드 (서명 필요)

### APK 위치
빌드 완료 후 APK 파일 위치:
```
app/build/outputs/apk/analytics/debug/app-analytics-debug.apk
app/build/outputs/apk/noAnalytics/debug/app-noAnalytics-debug.apk
```

### 로그 확인
- **Logcat** 창에서 필터링:
  - `package:mine` - 현재 앱 로그만
  - `tag:SetlOne` - SetlOne 태그 로그만

---

## 🚀 빠른 시작 체크리스트

- [ ] Android Studio에서 프로젝트 열기
- [ ] Gradle 동기화 완료 대기
- [ ] Build Variant 선택 (analyticsDebug 또는 noAnalyticsDebug)
- [ ] 에뮬레이터 시작 또는 연결된 기기 확인
- [ ] Run 또는 Debug 버튼 클릭
- [ ] 앱이 에뮬레이터에서 실행되는지 확인

---

## 💡 팁

1. **빠른 실행**: 에뮬레이터를 미리 실행해두면 앱 실행이 더 빠릅니다
2. **Hot Reload**: 코드 수정 후 `Ctrl + F9`로 빠르게 재빌드
3. **Logcat 필터**: 자주 사용하는 필터를 저장해두면 편리합니다
4. **디버그 설정**: `Run → Edit Configurations`에서 실행 설정 커스터마이징 가능
