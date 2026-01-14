# 크래시 분석 보고서

## 🔴 주요 크래시 (FATAL EXCEPTION)

### 1. OkHttp Dispatcher 크래시 (메인 크래시)

**위치**: `error.log` 라인 433-455

```
FATAL EXCEPTION: OkHttp Dispatcher
Process: com.setlone.wallet, PID: 14764
java.lang.IllegalStateException: Unreadable ResponseBody! These Response objects have bodies that are stripped:
 * Response.cacheResponse
 * Response.networkResponse
 * Response.priorResponse
 * EventSourceListener
 * WebSocketListener
```

**발생 위치**:
```
at okhttp3.internal.UnreadableResponseBody.read(UnreadableResponseBody.kt:41)
at okio.RealBufferedSource.select(RealBufferedSource.kt:255)
at okhttp3.internal._UtilJvmKt.readBomAsCharset(-UtilJvm.kt:88)
at okhttp3.ResponseBody.string(ResponseBody.kt:190)
at com.pandulapeter.beagle.logOkHttp.OkHttpInterceptor.intercept(OkHttpInterceptor.kt:63)
```

**원인 분석**:
- **Beagle 디버깅 라이브러리**의 `OkHttpInterceptor`가 이미 읽은 ResponseBody를 다시 읽으려고 시도
- WalletConnect의 WebSocket/EventSource 리스너에서 사용된 ResponseBody는 읽을 수 없음
- Beagle이 모든 HTTP 응답을 로깅하려고 시도하면서 문제 발생

**해결 방법**:
1. **Beagle을 디버그 빌드에서만 사용** (권장)
2. **OkHttpInterceptor 제외 설정** - WalletConnect 관련 요청 제외
3. **Beagle 버전 업데이트** - 최신 버전에서 수정되었을 수 있음

---

### 2. NullPointerException (BaseViewModel)

**위치**: `error.log` 라인 175, 344, 367, 385, 706

```
BaseViewModel: java.lang.NullPointerException: The callable returned a null value
at io.reactivex.internal.functions.ObjectHelper.requireNonNull(ObjectHelper.java:39)
at io.reactivex.internal.operators.single.SingleFromCallable.subscribeActual(SingleFromCallable.java:44)
```

**원인 분석**:
- RxJava의 `SingleFromCallable`에서 null 값을 반환
- `BaseViewModel`에서 사용하는 비동기 작업이 null을 반환
- 앱이 크래시되지는 않지만 에러 로그 발생

**해결 방법**:
- `BaseViewModel`에서 null 체크 추가
- `Single.fromCallable()` 대신 `Single.just()` 또는 `Single.error()` 사용

---

### 3. WalletConnect 연결 오류

**위치**: `error.log` 라인 411

```
AWWalletConnectClient: com.walletconnect.android.internal.common.exception.GenericException: 
Error while connecting, please check your Internet connection or contact support: 
java.io.IOException: canceled due to java.lang.IllegalStateException: Unreadable ResponseBody!
```

**원인 분석**:
- WalletConnect 연결 시도 중 Beagle의 OkHttpInterceptor와 충돌
- WebSocket/EventSource ResponseBody를 읽으려고 시도하여 오류 발생

---

## 🔍 크래시 발생 순서

1. **앱 시작** (라인 10): `HomeActivity` 시작
2. **SplashActivity 시작** (라인 341): `SplashActivity`로 전환
3. **NullPointerException 발생** (라인 175, 344, 367, 385): 여러 번 발생
4. **WalletConnect 연결 시도** (라인 411): 연결 오류 발생
5. **FATAL EXCEPTION** (라인 433): OkHttp Dispatcher에서 크래시
6. **앱 강제 종료** (라인 458): `Force finishing activity`
7. **프로세스 종료** (라인 463): `Sending signal. PID: 14764 SIG: 9`

---

## ✅ 해결 방법

### 즉시 해결 (우선순위: 높음)

#### 1. Beagle 디버그 빌드에서만 사용

**파일**: `app/build.gradle`

```gradle
dependencies {
    // Debug 빌드에서만 Beagle 사용
    debugImplementation 'com.pandulapeter.beagle:beagle:버전'
    // 또는 완전히 제거
    // debugImplementation 'com.pandulapeter.beagle:beagle:버전'
}
```

**또는 Application 클래스에서**:

```java
if (BuildConfig.DEBUG) {
    // Beagle 초기화
} else {
    // Beagle 초기화 안 함
}
```

#### 2. Beagle OkHttpInterceptor 제외 설정

Beagle 설정에서 WalletConnect 관련 요청 제외:

```kotlin
Beagle.setup {
    logOkHttp {
        // 특정 URL 패턴 제외
        ignoreUrlPatterns = listOf(
            ".*walletconnect.*",
            ".*relay.*"
        )
    }
}
```

#### 3. Beagle 완전 제거 (임시 해결)

디버깅이 필요하지 않다면 Beagle을 완전히 제거:

```gradle
// build.gradle에서 Beagle 의존성 제거
// dependencies {
//     debugImplementation 'com.pandulapeter.beagle:beagle:...'
// }
```

---

### 추가 해결 (우선순위: 중간)

#### 4. BaseViewModel Null 체크

`BaseViewModel`에서 null 반환 방지:

```java
// Single.fromCallable() 사용 시 null 체크
Single.fromCallable(() -> {
    Object result = someMethod();
    if (result == null) {
        throw new IllegalStateException("Result cannot be null");
    }
    return result;
})
```

---

## 📊 크래시 통계

- **FATAL EXCEPTION**: 1건 (OkHttp Dispatcher)
- **NullPointerException**: 5건 (BaseViewModel)
- **WalletConnect 오류**: 1건
- **앱 강제 종료**: 1건

---

## 🎯 권장 조치

### 즉시 조치
1. ✅ **Beagle을 디버그 빌드에서만 사용하거나 제거**
2. ✅ **앱 재빌드 및 테스트**

### 추가 조치
3. ⚠️ **BaseViewModel null 체크 추가**
4. ⚠️ **Beagle 최신 버전 확인 및 업데이트**

---

## 🔗 참고

- Beagle은 디버깅 도구이므로 프로덕션 빌드에서는 사용하지 않는 것이 좋습니다
- WalletConnect는 WebSocket을 사용하므로 일반 HTTP 인터셉터와 호환되지 않을 수 있습니다
