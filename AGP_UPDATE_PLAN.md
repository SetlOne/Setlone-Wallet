# AGP 및 16KB 페이지 크기 지원 업데이트 계획

## 📋 현재 버전 상태

### 현재 사용 중인 버전
- **AGP (Android Gradle Plugin)**: `8.13.2`
- **Gradle**: `8.13.2`
- **compileSdk**: `35` ✅
- **targetSdk**: `34` ⚠️ (35로 업데이트 필요)
- **minSdk**: `24` ✅

### 참고 문서
[Android 16KB 페이지 크기 가이드](https://developer.android.com/guide/practices/page-sizes?hl=ko)

---

## 🎯 업데이트 필요 사항

### 1. targetSdk 업데이트 (필수) ⚠️

**현재**: `targetSdk 34`  
**필요**: `targetSdk 35`

**이유**:
- Google Play 요구사항: 2025년 11월 1일부터 Android 15 (API 35) 이상을 타겟팅하는 앱은 16KB 페이지 크기를 지원해야 함
- 현재 날짜: 2026년 1월 14일 → **이미 필수 요구사항 적용됨**

**수정 위치**: `app/build.gradle`
```gradle
defaultConfig {
    targetSdk 35  // 34 → 35로 변경
}
```

### 2. AGP 버전 확인 및 업데이트

**현재**: `8.13.2`  
**최신**: 확인 필요 (2026년 1월 기준)

**AGP 8.13.x 시리즈 특징**:
- 16KB 페이지 크기 지원 개선
- Android 15 (API 35) 완전 지원
- 빌드 성능 개선

**확인 필요**:
- AGP 8.14.x 또는 8.15.x가 출시되었는지 확인
- Gradle 8.13.2와 호환성 확인

### 3. Gradle 버전 확인

**현재**: `8.13.2`  
**상태**: AGP 8.13.2와 호환됨 ✅

**참고**: AGP 8.13.x는 Gradle 8.13.x와 함께 사용 권장

---

## 📝 업데이트 계획

### 단계 1: targetSdk 업데이트 (우선순위: 높음)

**파일**: `app/build.gradle`

```gradle
defaultConfig {
    applicationId "com.setlone.wallet"
    minSdk 24
    targetSdk 35  // 34 → 35로 변경
    versionCode 273
    versionName "3.88"
    // ...
}
```

**주의사항**:
- targetSdk 35로 업데이트 시 Android 15의 새로운 동작 변경사항 적용됨
- 테스트 필요:
  - 권한 동작 변경
  - 백그라운드 제한
  - 보안 정책 변경

### 단계 2: AGP 최신 버전 확인 및 업데이트 (우선순위: 중간)

**파일**: `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.13.2"  # 또는 최신 버전 (8.14.x, 8.15.x 등)
gradle = "8.13.2"  # AGP와 호환되는 버전
```

**확인 방법**:
1. [Android Gradle Plugin 릴리스 노트](https://developer.android.com/studio/releases/gradle-plugin) 확인
2. 최신 안정 버전 확인
3. 호환성 테스트

### 단계 3: 16KB 페이지 크기 지원 확인

**이미 적용된 설정**:
- ✅ CMakeLists.txt에 링커 플래그 추가
- ✅ packagingOptions 업데이트

**추가 확인 사항**:
- 외부 라이브러리 (TrustWalletCore, SQLCipher) 16KB 지원 버전 확인
- APK 빌드 후 16KB 호환성 검증

---

## 🔍 공식 문서 요구사항 요약

### Google Play 호환성 요구사항
> **2025년 11월 1일부터** Google Play에 제출되고 **Android 15 (API 수준 35) 이상을 실행하는 기기를 타겟팅**하는 모든 신규 앱과 기존 앱 업데이트는 **16KB 페이지 크기를 지원**해야 합니다.

### 현재 상황 (2026년 1월 14일)
- ✅ **이미 필수 요구사항 적용됨**
- ⚠️ **targetSdk 35로 업데이트 필요**
- ⚠️ **16KB 페이지 크기 지원 필수**

---

## 🛠️ 구체적 업데이트 작업

### 1. targetSdk 업데이트

**파일**: `app/build.gradle`

**변경 전**:
```gradle
targetSdk 34
```

**변경 후**:
```gradle
targetSdk 35
```

### 2. AGP 버전 확인

**현재 AGP 8.13.2 사용 중**:
- Android 15 (API 35) 지원 ✅
- 16KB 페이지 크기 지원 ✅
- 최신 안정 버전에 가까움

**업데이트 필요 여부**:
- AGP 8.14.x 이상이 출시되었는지 확인
- 출시되었다면 업데이트 고려
- 출시되지 않았다면 현재 버전 유지

### 3. Gradle 버전 확인

**현재 Gradle 8.13.2**:
- AGP 8.13.2와 완벽 호환 ✅
- 업데이트 불필요 (AGP와 함께 업데이트)

---

## ⚠️ 주의사항

### targetSdk 35 업데이트 시 확인 사항

1. **권한 변경**
   - Android 15의 새로운 권한 정책 확인
   - 런타임 권한 요청 동작 변경

2. **백그라운드 제한**
   - 백그라운드 작업 제한 강화
   - Foreground Service 타입 명시 필요

3. **보안 정책**
   - 파일 접근 정책 변경
   - 네트워크 보안 정책 강화

4. **테스트**
   - Android 15 에뮬레이터에서 테스트
   - 실제 기기에서 테스트 (가능한 경우)

---

## 📊 업데이트 우선순위

### 즉시 필요 (필수)
1. ✅ **targetSdk 35로 업데이트** - Google Play 제출 필수
2. ✅ **16KB 페이지 크기 지원 확인** - 이미 적용됨

### 확인 필요 (권장)
3. ⚠️ **AGP 최신 버전 확인** - 8.14.x 이상 출시 여부 확인
4. ⚠️ **외부 라이브러리 업데이트** - TrustWalletCore, SQLCipher 최신 버전 확인

### 선택 사항
5. 📝 **Gradle 최신 버전** - AGP와 함께 업데이트 (필요 시)

---

## 🔗 참고 링크

- [Android 16KB 페이지 크기 가이드](https://developer.android.com/guide/practices/page-sizes?hl=ko)
- [Android Gradle Plugin 릴리스 노트](https://developer.android.com/studio/releases/gradle-plugin)
- [Android 15 변경사항](https://developer.android.com/about/versions/15)
- [Google Play 16KB 요구사항](https://developer.android.com/guide/practices/page-sizes#play-store-requirements)

---

## ✅ 다음 단계

1. **targetSdk 35로 업데이트** (즉시)
2. **AGP 최신 버전 확인** (공식 문서 확인)
3. **빌드 및 테스트** (Android 15 에뮬레이터)
4. **16KB 호환성 검증** (APK 분석)
5. **Google Play Console 사전 검사** (제출 전)
