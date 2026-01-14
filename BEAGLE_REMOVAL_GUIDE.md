# Beagle 제거 가이드

## 🔍 Beagle이란?

**Beagle**은 Android 앱 개발을 위한 **디버깅 도구**입니다.

### 주요 기능
- HTTP 요청/응답 로깅
- SharedPreferences, 데이터베이스 내용 확인
- 네트워크 상태 모니터링
- 앱 내 디버깅 UI 제공

### 특징
- **디버깅 전용**: 프로덕션 빌드에서는 사용하지 않음
- **선택적 도구**: 필수 라이브러리가 아님
- **대체 가능**: 다른 디버깅 도구로 대체 가능

---

## ✅ 제거해도 되는가?

### **네, 제거해도 됩니다!**

**이유**:
1. ✅ **디버깅 전용 도구**: 앱의 핵심 기능과 무관
2. ✅ **선택적 도구**: 필수 라이브러리가 아님
3. ✅ **크래시 원인**: 현재 앱 크래시의 주요 원인
4. ✅ **대체 가능**: Logcat, Timber 등으로 대체 가능

### 제거 시 영향
- ❌ **기능적 영향 없음**: 앱의 핵심 기능에 영향 없음
- ❌ **사용자 영향 없음**: 사용자는 Beagle을 볼 수 없음
- ✅ **크래시 해결**: WalletConnect와의 충돌 해결
- ✅ **앱 안정성 향상**: 불필요한 디버깅 도구 제거

---

## 🔧 제거 방법

### 방법 1: 의존성에서 제거 (권장)

**파일**: `app/build.gradle`

```gradle
dependencies {
    // Beagle 제거 (있다면)
    // debugImplementation 'com.pandulapeter.beagle:beagle:...'
    // 또는
    // implementation 'com.pandulapeter.beagle:beagle:...'
}
```

### 방법 2: 코드에서 초기화 제거

**파일**: `App.java` 또는 초기화하는 곳

```java
// Beagle 초기화 코드 제거 (있다면)
// Beagle.setup { ... }
```

### 방법 3: 다른 라이브러리의 의존성으로 포함된 경우

**확인 방법**:
```powershell
.\gradlew.bat :app:dependencies --configuration noAnalyticsDebugRuntimeClasspath | Select-String "beagle"
```

**제외 방법**:
```gradle
implementation("com.walletconnect:android-core", {
    exclude group: 'com.pandulapeter.beagle', module: '*'
    exclude group: 'org.web3j', module: '*'
    exclude group: 'org.bouncycastle', module: '*'
})
```

---

## 🔄 대체 방법

### 1. Logcat 사용 (기본 제공)

**장점**:
- Android Studio 기본 제공
- 추가 라이브러리 불필요
- 모든 로그 확인 가능

**사용법**:
- Android Studio → Logcat 탭
- 필터: `package:mine`
- HTTP 로그는 OkHttp의 LogInterceptor 사용

### 2. Timber 사용 (이미 프로젝트에 있음)

**현재 사용 중**: `TimberInit.configTimber()`

**장점**:
- 이미 프로젝트에 포함됨
- 릴리스 빌드에서 자동 비활성화 가능
- 커스텀 로깅 가능

**확인**: `app/src/main/java/com/setlone/app/util/TimberInit.kt`

### 3. OkHttp LogInterceptor (이미 있음)

**파일**: `app/src/main/java/com/setlone/app/util/LogInterceptor.java`

**현재 상태**: 주석 처리됨
```java
//.addInterceptor(new LogInterceptor())
```

**활성화 방법**:
```java
OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder()
            .addInterceptor(new LogInterceptor())  // 주석 해제
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            // ...
}
```

**장점**:
- 프로젝트에 이미 있음
- HTTP 요청/응답 로깅 가능
- WalletConnect와 충돌 없음

### 4. Chrome DevTools (WebView 디버깅)

**사용법**:
- `chrome://inspect` 접속
- WebView 디버깅 가능

---

## 📊 비교표

| 기능 | Beagle | Logcat + Timber | LogInterceptor |
|------|--------|----------------|----------------|
| HTTP 로깅 | ✅ | ✅ (LogInterceptor) | ✅ |
| 앱 내 UI | ✅ | ❌ | ❌ |
| 크래시 위험 | ⚠️ (WalletConnect 충돌) | ✅ 안전 | ✅ 안전 |
| 추가 라이브러리 | 필요 | 불필요 | 불필요 |
| 프로덕션 사용 | ❌ | ✅ (조건부) | ✅ (조건부) |

---

## 🎯 권장 사항

### 즉시 조치
1. ✅ **Beagle 제거** - 크래시 해결
2. ✅ **LogInterceptor 활성화** (디버그 빌드에서만)
3. ✅ **Timber 계속 사용** - 이미 잘 작동 중

### LogInterceptor 활성화 예시

**파일**: `app/src/main/java/com/setlone/app/di/ToolsModule.java`

```java
@Singleton
@Provides
OkHttpClient okHttpClient()
{
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false);
    
    // 디버그 빌드에서만 LogInterceptor 추가
    if (BuildConfig.DEBUG) {
        builder.addInterceptor(new LogInterceptor());
    }
    
    return builder.build();
}
```

---

## ✅ 결론

### 제거해도 되는가?
**네, 제거해도 됩니다!**
- 기능적 영향 없음
- 크래시 해결
- 대체 방법 충분

### 대체 방법
1. **Logcat** (기본 제공) ✅
2. **Timber** (이미 사용 중) ✅
3. **LogInterceptor** (이미 있음, 활성화만) ✅

### 권장 조치
1. Beagle 제거
2. LogInterceptor 활성화 (디버그 빌드)
3. Timber 계속 사용

---

## 🔗 참고

- Beagle은 디버깅 편의 도구일 뿐, 필수 라이브러리가 아닙니다
- 프로덕션 빌드에서는 사용하지 않는 것이 좋습니다
- Logcat과 Timber만으로도 충분한 디버깅이 가능합니다
