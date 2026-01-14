# Beagle 제거 솔루션

## 🔍 발견 사항

**Beagle은 WalletConnect 라이브러리의 의존성으로 포함되어 있습니다.**

```
io.github.pandulapeter.beagle:log-okhttp:2.9.0
```

이것은 WalletConnect가 Beagle을 자동으로 포함시킨 것입니다.

---

## ✅ 제거해도 되는가?

### **네, 제거해도 됩니다!**

**이유**:
1. ✅ **디버깅 전용**: Beagle은 디버깅 도구일 뿐, 앱 기능과 무관
2. ✅ **선택적 도구**: WalletConnect가 정상 작동하는데 필수 아님
3. ✅ **크래시 원인**: 현재 앱 크래시의 주요 원인
4. ✅ **대체 가능**: Logcat, Timber, LogInterceptor로 충분

### 제거 시 영향
- ❌ **기능적 영향 없음**: 앱의 핵심 기능에 영향 없음
- ❌ **WalletConnect 영향 없음**: WalletConnect는 정상 작동
- ✅ **크래시 해결**: WebSocket ResponseBody 충돌 해결
- ✅ **앱 안정성 향상**: 불필요한 디버깅 도구 제거

---

## 🔧 제거 방법

### WalletConnect 의존성에서 Beagle 제외

**파일**: `app/build.gradle`

**현재 코드** (라인 300-308):
```gradle
implementation platform(libs.walletConnect.bom)
implementation("com.walletconnect:android-core", {
    exclude group: 'org.web3j', module: '*'
    exclude group: 'org.bouncycastle', module: '*'
})
implementation("com.walletconnect:web3wallet", {
    exclude group: 'org.web3j', module: '*'
    exclude group: 'org.bouncycastle', module: '*'
})
```

**수정 후**:
```gradle
implementation platform(libs.walletConnect.bom)
implementation("com.walletconnect:android-core", {
    exclude group: 'org.web3j', module: '*'
    exclude group: 'org.bouncycastle', module: '*'
    exclude group: 'io.github.pandulapeter.beagle', module: '*'  // 추가
})
implementation("com.walletconnect:web3wallet", {
    exclude group: 'org.web3j', module: '*'
    exclude group: 'org.bouncycastle', module: '*'
    exclude group: 'io.github.pandulapeter.beagle', module: '*'  // 추가
})
```

---

## 🔄 대체 방법

### 1. Logcat (기본 제공) ✅

**장점**:
- Android Studio 기본 제공
- 추가 라이브러리 불필요
- 모든 로그 확인 가능

**사용법**:
- Android Studio → Logcat 탭
- 필터: `package:mine` 또는 `package:com.setlone.wallet`

### 2. Timber (이미 사용 중) ✅

**현재 상태**: `TimberInit.configTimber()` 사용 중

**장점**:
- 이미 프로젝트에 포함됨
- 릴리스 빌드에서 자동 비활성화 가능
- 커스텀 로깅 가능

**파일**: `app/src/main/java/com/setlone/app/util/TimberInit.kt`

### 3. LogInterceptor (이미 있음, 활성화만) ✅

**파일**: `app/src/main/java/com/setlone/app/util/LogInterceptor.java`

**현재 상태**: 주석 처리됨
```java
//.addInterceptor(new LogInterceptor())
```

**활성화 방법**:

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

**장점**:
- 프로젝트에 이미 있음
- HTTP 요청/응답 로깅 가능
- WalletConnect와 충돌 없음
- 디버그 빌드에서만 활성화 가능

---

## 📊 비교표

| 기능 | Beagle | Logcat + Timber | LogInterceptor |
|------|--------|----------------|----------------|
| HTTP 로깅 | ✅ | ✅ (LogInterceptor) | ✅ |
| 앱 내 UI | ✅ | ❌ | ❌ |
| 크래시 위험 | ⚠️ (WalletConnect 충돌) | ✅ 안전 | ✅ 안전 |
| 추가 라이브러리 | 필요 (의존성) | 불필요 | 불필요 |
| 프로덕션 사용 | ❌ | ✅ (조건부) | ✅ (조건부) |
| WalletConnect 호환 | ❌ 충돌 | ✅ 호환 | ✅ 호환 |

---

## 🎯 권장 조치

### 즉시 조치 (우선순위: 높음)

1. ✅ **Beagle 제외** - WalletConnect 의존성에서 제외
2. ✅ **LogInterceptor 활성화** - 디버그 빌드에서만
3. ✅ **앱 재빌드 및 테스트**

### 단계별 실행

#### Step 1: Beagle 제외
`app/build.gradle`에서 WalletConnect 의존성에 Beagle 제외 추가

#### Step 2: LogInterceptor 활성화 (선택)
디버그 빌드에서 HTTP 로깅이 필요하면 활성화

#### Step 3: 빌드 및 테스트
```powershell
.\gradlew.bat clean assembleNoAnalyticsDebug
```

---

## ✅ 결론

### 제거해도 되는가?
**네, 제거해도 됩니다!**
- ✅ 기능적 영향 없음
- ✅ WalletConnect 정상 작동
- ✅ 크래시 해결
- ✅ 대체 방법 충분

### 대체 방법
1. **Logcat** (기본 제공) ✅
2. **Timber** (이미 사용 중) ✅
3. **LogInterceptor** (이미 있음, 활성화만) ✅

### 권장 조치
1. ✅ Beagle 제외 (WalletConnect 의존성에서)
2. ✅ LogInterceptor 활성화 (디버그 빌드)
3. ✅ Timber 계속 사용

---

## 🔗 참고

- Beagle은 디버깅 편의 도구일 뿐, 필수 라이브러리가 아닙니다
- WalletConnect는 Beagle 없이도 정상 작동합니다
- Logcat과 Timber만으로도 충분한 디버깅이 가능합니다
- LogInterceptor는 프로젝트에 이미 있으므로 추가 작업 없이 사용 가능합니다
