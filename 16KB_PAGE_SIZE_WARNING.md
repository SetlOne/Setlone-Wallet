# 16KB 페이지 크기 호환성 경고 해결 가이드

## ⚠️ 경고 내용

```
APK app-noAnalytics-debug.apk is not compatible with 16 KB devices. 
Some libraries have LOAD segments not aligned at 16 KB boundaries:
- lib/arm64-v8a/libTrustWalletCore.so
- lib/arm64-v8a/libsqlcipher.so
- lib/x86_64/libTrustWalletCore.so
- lib/x86_64/libkeys.so
- lib/x86_64/libsqlcipher.so
```

## 📖 경고 의미

### 무엇인가요?
- **16KB 페이지 크기**: 일부 최신 Android 디바이스(특히 Android 15+)는 16KB 메모리 페이지를 사용합니다
- **기본값**: 대부분의 Android 디바이스는 4KB 페이지를 사용합니다
- **문제**: 일부 네이티브 라이브러리(.so 파일)가 16KB 경계에 정렬되지 않아 호환되지 않습니다

### 왜 중요한가요?
- **2025년 11월 1일부터**: Google Play에 제출하는 모든 새 앱과 업데이트는 Android 15+ 타겟팅 시 16KB 페이지 크기를 지원해야 합니다
- **현재 상태**: 경고이므로 앱은 실행되지만, 향후 Google Play 제출 시 거부될 수 있습니다

### 영향받는 라이브러리
1. **libTrustWalletCore.so** - Trust Wallet Core 라이브러리
2. **libsqlcipher.so** - SQLCipher 암호화 라이브러리
3. **libkeys.so** - 키 관리 라이브러리

---

## ✅ 해결 방법

### 방법 1: 라이브러리 업데이트 (권장)

#### TrustWalletCore 업데이트
최신 버전의 TrustWalletCore는 16KB 페이지 크기를 지원합니다.

1. `gradle/libs.versions.toml` 또는 `app/build.gradle`에서 TrustWalletCore 버전 확인
2. 최신 버전으로 업데이트 (가능한 경우)

#### SQLCipher 업데이트
SQLCipher의 최신 버전도 16KB를 지원합니다.

1. 현재 사용 중인 SQLCipher 버전 확인
2. 최신 버전으로 업데이트

### 방법 2: NDK 빌드 설정 추가

`app/build.gradle`의 `defaultConfig` 섹션에 다음 추가:

```gradle
defaultConfig {
    // ... 기존 설정 ...
    
    ndk {
        abiFilters "armeabi-v7a", "x86", "x86_64", "arm64-v8a"
    }
    
    // 16KB 페이지 크기 지원 추가
    externalNativeBuild {
        cmake {
            arguments "-DANDROID_SUPPORT_16KB_PAGE_SIZE=ON"
        }
    }
}
```

### 방법 3: CMakeLists.txt 수정

`app/src/main/cpp/CMakeLists.txt`에 16KB 정렬 설정 추가:

```cmake
# 16KB 페이지 크기 지원
if(ANDROID_SUPPORT_16KB_PAGE_SIZE)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,max-page-size=16384")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,max-page-size=16384")
endif()
```

### 방법 4: packagingOptions에 정렬 설정 추가

`app/build.gradle`의 `packagingOptions`에 추가:

```gradle
packagingOptions {
    // ... 기존 설정 ...
    
    jniLibs {
        useLegacyPackaging = false
    }
}
```

---

## 🔍 현재 상태 확인

### 즉시 조치 필요 여부
- ❌ **아니요**: 현재는 경고일 뿐이며 앱 실행에는 문제가 없습니다
- ⚠️ **향후 필요**: 2025년 11월 1일 이후 Google Play 제출 시 필수

### 우선순위
1. **낮음 (현재)**: 개발 및 테스트 단계에서는 무시해도 됩니다
2. **중간 (2025년 중반)**: Google Play 제출 전에 해결 필요
3. **높음 (2025년 11월 이후)**: 필수 요구사항

---

## 🛠️ 권장 조치 사항

### 단기 (지금 할 필요 없음)
- 경고는 무시하고 개발 계속 진행 가능
- 앱 실행 및 기능 테스트에는 영향 없음

### 중기 (2025년 중반)
1. TrustWalletCore 최신 버전 확인 및 업데이트
2. SQLCipher 최신 버전 확인 및 업데이트
3. NDK 빌드 설정에 16KB 지원 추가
4. 테스트 빌드 후 경고 해결 확인

### 장기 (Google Play 제출 전)
1. 16KB 페이지 크기 디바이스에서 테스트
2. Google Play Console의 사전 검사 통과 확인
3. 모든 네이티브 라이브러리 호환성 확인

---

## 📝 참고 자료

- [Android 16KB 페이지 크기 가이드](https://developer.android.com/16kb-page-size)
- [Google Play 16KB 요구사항](https://developer.android.com/16kb-page-size#play-store-requirements)
- [TrustWalletCore GitHub](https://github.com/trustwallet/wallet-core)
- [SQLCipher 문서](https://www.zetetic.net/sqlcipher/)

---

## 💡 요약

**현재 상태**: 경고이지만 실행에는 문제 없음
**조치 필요 시기**: 2025년 11월 1일 이전 (Google Play 제출 전)
**우선순위**: 낮음 (개발 단계에서는 무시 가능)
**해결 방법**: 라이브러리 업데이트 또는 NDK 빌드 설정 수정

**결론**: 지금 당장 조치할 필요는 없지만, 향후 Google Play 제출 전에는 반드시 해결해야 합니다.
