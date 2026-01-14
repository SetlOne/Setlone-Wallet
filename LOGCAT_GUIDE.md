# Android Studio Logcat 가이드 - 크래시 디버깅

## 🔍 Logcat 창 열기

### 방법 1: 하단 툴바에서 열기
1. Android Studio 하단의 **Logcat** 탭 클릭
2. 없으면: **View → Tool Windows → Logcat**

### 방법 2: 단축키
- Windows/Linux: `Alt + 6`
- Mac: `Cmd + 6`

---

## 📱 크래시 로그 확인하기

### 1. 필터 설정 (중요!)

#### 패키지 필터 (가장 유용)
```
package:mine
```
- 현재 앱의 로그만 표시
- 다른 앱의 로그 제거

#### 태그 필터
```
tag:SetlOne
```
- SetlOne 태그가 있는 로그만 표시

#### 레벨 필터
- **Error**: `level:error` - 에러만 표시
- **Warning**: `level:warning` - 경고만 표시
- **Debug**: `level:debug` - 디버그 로그만 표시

#### 크래시 로그 찾기
```
AndroidRuntime
```
- 크래시 스택 트레이스 확인

### 2. 복합 필터 (권장)
```
package:mine level:error
```
또는
```
package:com.setlone.wallet
```

---

## 🚨 크래시 로그 분석

### 일반적인 크래시 패턴

#### 1. NullPointerException
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.setlone.wallet, PID: 12345
    java.lang.NullPointerException: Attempt to invoke virtual method...
```

#### 2. ClassNotFoundException
```
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.ClassNotFoundException: Didn't find class "com.setlone.app..."
```

#### 3. IllegalStateException
```
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.IllegalStateException: ...
```

---

## 🔧 Logcat 설정

### 1. 로그 레벨 선택
- 상단 드롭다운에서 레벨 선택:
  - **Verbose** (모든 로그)
  - **Debug** (디버그 이상)
  - **Info** (정보 이상)
  - **Warn** (경고 이상)
  - **Error** (에러만)

### 2. 디바이스 선택
- 상단 드롭다운에서 연결된 디바이스/에뮬레이터 선택

### 3. 검색 기능
- 검색창에 키워드 입력 (예: "crash", "error", "exception")

---

## 📋 유용한 필터 프리셋

### 크래시만 보기
```
package:mine level:error tag:AndroidRuntime
```

### SetlOne 앱 로그만
```
package:com.setlone.wallet
```

### 에러와 경고만
```
package:mine level:error || level:warning
```

### 특정 클래스 로그
```
tag:HomeActivity
```

---

## 🛠️ 실시간 디버깅 팁

### 1. 앱 실행 전 Logcat 준비
1. Logcat 창 열기
2. 필터 설정: `package:mine`
3. 레벨: **Verbose** 또는 **Debug**
4. 앱 실행

### 2. 크래시 발생 시
1. Logcat에서 빨간색 에러 로그 확인
2. `FATAL EXCEPTION` 검색
3. 스택 트레이스 전체 복사
4. 원인 분석

### 3. 로그 저장
- **File → Save Logcat to File...**
- 크래시 로그를 파일로 저장하여 분석

---

## 🎯 빠른 시작 체크리스트

- [ ] Logcat 창 열기 (Alt + 6)
- [ ] 필터 설정: `package:mine`
- [ ] 레벨: **Verbose** 또는 **Error**
- [ ] 디바이스 선택 (에뮬레이터/실제 기기)
- [ ] 앱 실행
- [ ] 크래시 발생 시 `FATAL EXCEPTION` 검색
- [ ] 스택 트레이스 확인

---

## 💡 추가 팁

### 1. 로그 색상 변경
- **File → Settings → Editor → Color Scheme → Android Logcat**
- 에러, 경고, 정보 등 색상 커스터마이징

### 2. 로그 정리
- Logcat 창에서 **Clear Logcat** 버튼 클릭
- 또는 `Ctrl + L` (Windows/Linux)

### 3. 로그 검색
- `Ctrl + F` (Windows/Linux) 또는 `Cmd + F` (Mac)
- 키워드로 검색

### 4. 여러 필터 저장
- 필터 설정 후 **+** 버튼으로 저장
- 나중에 빠르게 전환 가능

---

## 🔍 일반적인 크래시 원인

### 1. 초기화 오류
- Application 클래스에서 크래시
- 의존성 주입 실패 (Hilt/Dagger)

### 2. 리소스 누락
- 레이아웃 파일 누락
- 리소스 ID 오류

### 3. 권한 문제
- 필수 권한 미승인
- 런타임 권한 오류

### 4. 네트워크/서비스 오류
- Firebase 초기화 실패
- 서비스 연결 실패

---

## 📞 문제 해결

### Logcat이 비어있는 경우
1. 디바이스/에뮬레이터 연결 확인
2. 디바이스 선택 확인
3. 필터가 너무 제한적인지 확인

### 로그가 너무 많은 경우
1. 필터 추가: `package:mine`
2. 레벨을 **Error** 또는 **Warn**으로 변경
3. 검색으로 특정 키워드 필터링

---

## 🎯 크래시 로그 예시

```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.setlone.wallet, PID: 12345
    java.lang.RuntimeException: Unable to start activity ComponentInfo{com.setlone.wallet/com.setlone.app.ui.HomeActivity}: 
    java.lang.NullPointerException: Attempt to invoke virtual method 'void com.setlone.app.service.SetlOneNotificationService.subscribe(long)' on a null object reference
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3449)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3601)
        ...
        Caused by: java.lang.NullPointerException: Attempt to invoke virtual method...
        at com.setlone.app.viewmodel.HomeViewModel.init(HomeViewModel.java:123)
        ...
```

이런 로그를 보면:
- **어디서**: `HomeViewModel.java:123`
- **무엇이**: `NullPointerException`
- **왜**: `SetlOneNotificationService`가 null

---

## ✅ 다음 단계

1. Logcat 열기
2. 필터 설정: `package:mine level:error`
3. 앱 실행
4. 크래시 로그 확인
5. 스택 트레이스 분석
6. 문제 해결
