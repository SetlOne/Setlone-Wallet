# 16KB 페이지 크기 추가 설정 가이드

## ✅ 이미 적용된 설정

1. **Gradle**: 9.2.1 ✅
2. **AGP**: 8.13.2 ✅
3. **NDK**: r29 (29.0.14206865) ✅
4. **CMakeLists.txt**: 16KB 정렬 플래그 추가 ✅
5. **packagingOptions**: `useLegacyPackaging = false` ✅
6. **AndroidManifest.xml**: `extractNativeLibs="false"` 추가 ✅

## ⚠️ 여전히 경고가 나타나는 이유

**외부 라이브러리(.so 파일)가 4KB 정렬로 빌드되어 있기 때문입니다.**

다음 라이브러리들이 문제일 수 있습니다:
- `libTrustWalletCore.so` (Trust Wallet Core 4.5.0)
- `libsqlcipher.so` (SQLCipher - Realm에서 사용)

## 🔧 추가 해결 방법

### 방법 1: 라이브러리 업데이트 확인

#### TrustWalletCore
- 현재 버전: 4.5.0
- 최신 버전 확인: https://github.com/trustwallet/wallet-core/releases
- 16KB 지원 버전이 있는지 확인

#### SQLCipher
- 현재: Realm 10.19.0에 포함된 버전 사용
- SQLCipher 4.5.9를 직접 추가했지만, Realm이 내부적으로 사용하는 버전과 충돌할 수 있음

### 방법 2: APK에서 실제 Alignment 확인

```powershell
# 1. APK 빌드
.\gradlew.bat assembleNoAnalyticsDebug

# 2. Alignment 확인 스크립트 실행
.\check_16kb_alignment.ps1
```

### 방법 3: Google Play Console에서 확인

1. AAB 파일 업로드 (APK가 아닌 AAB)
2. App Bundle Explorer에서 확인
3. "Memory page size" 필드 확인

### 방법 4: 외부 라이브러리 제외 (임시)

만약 특정 라이브러리가 계속 문제를 일으킨다면:

```gradle
packagingOptions {
    jniLibs {
        // 특정 ABI 제외 (32비트는 4KB 정렬일 가능성 높음)
        exclude 'lib/armeabi-v7a/libTrustWalletCore.so'
        exclude 'lib/x86/libTrustWalletCore.so'
        // 또는 64비트만 사용
        // abiFilters "arm64-v8a", "x86_64"
    }
}
```

## 📊 현재 상태

### 우리가 빌드하는 라이브러리
- ✅ `libkeys.so` - 16KB 정렬 적용됨 (CMakeLists.txt 설정)

### 외부 라이브러리
- ⚠️ `libTrustWalletCore.so` - 라이브러리 제공자가 업데이트 필요
- ⚠️ `libsqlcipher.so` - Realm/SQLCipher 업데이트 필요

## 💡 중요 참고사항

1. **AGP 8.5.1+는 자동으로 uncompressed .so 파일을 16KB zip-aligned로 패키징합니다.**
   - 하지만 이것은 **zip alignment**일 뿐, **ELF segment alignment**는 아닙니다.
   - ELF segment alignment는 라이브러리를 빌드할 때 결정됩니다.

2. **외부 라이브러리의 ELF alignment를 변경하려면:**
   - 라이브러리 제공자가 16KB 정렬로 재빌드해야 합니다.
   - 또는 라이브러리 소스를 직접 빌드해야 합니다.

3. **Google Play Console의 경고:**
   - 일부 외부 라이브러리 경고는 허용될 수 있습니다.
   - 하지만 가능한 한 모두 해결하는 것이 좋습니다.

## 🎯 다음 단계

1. APK를 빌드하고 `check_16kb_alignment.ps1` 스크립트로 실제 alignment 확인
2. 문제가 되는 .so 파일 식별
3. 해당 라이브러리의 최신 버전 확인 및 업데이트
4. 여전히 문제가 있다면 라이브러리 제공자에게 문의
