# 권한 및 Play Store 준수성 검토 보고서

## 📋 개요
- **targetSdk**: 35 (Android 15)
- **minSdk**: 24 (Android 7.0)
- **빌드 상태**: ✅ 성공
- **Play Store 준수성**: ⚠️ 일부 수정 필요

---

## ✅ 잘 구현된 부분

### 1. POST_NOTIFICATIONS 권한 (Android 13+)
- ✅ `ActivityResultLauncher` 사용 (최신 방식)
- ✅ `PermissionRationaleDialog`로 사용자 설명 제공
- ✅ AndroidManifest에 권한 선언됨
- ✅ API 33 이상에서만 요청

**위치**: `PermissionUtils.java`, `NotificationSettingsActivity.java`

---

## ⚠️ 수정이 필요한 부분

### 1. READ_EXTERNAL_STORAGE 권한 문제

#### 문제점
- ❌ AndroidManifest.xml에 `READ_EXTERNAL_STORAGE` 권한이 선언되지 않음
- ❌ 코드에서 사용 중 (`DappBrowserFragment.java`, `AssetDefinitionService.java`)
- ❌ Android 13+ (API 33+)에서는 deprecated됨

#### Android 13+ 변경사항
- `READ_EXTERNAL_STORAGE` → 다음 중 하나로 변경 필요:
  - `READ_MEDIA_IMAGES` (이미지)
  - `READ_MEDIA_VIDEO` (비디오)
  - `READ_MEDIA_AUDIO` (오디오)
  - 또는 `READ_MEDIA_VISUAL_USER_SELECTED` (사용자 선택 미디어)

#### 해결 방법
```xml
<!-- AndroidManifest.xml에 추가 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

```java
// 코드 수정 필요
private String[] getStoragePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
    } else {
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }
}
```

---

### 2. 구식 권한 요청 방식 사용

#### 문제점
- ❌ `requestPermissions()` 사용 (deprecated)
- ❌ `onRequestPermissionsResult()` 사용 (deprecated)
- ✅ `ActivityResultLauncher`로 마이그레이션 필요

#### 영향받는 파일
1. **DappBrowserFragment.java**
   - `checkReadPermission()` - READ_EXTERNAL_STORAGE
   - `requestGeoPermission()` - ACCESS_FINE_LOCATION
   - `requestCameraPermission()` - CAMERA

2. **QRScannerActivity.java**
   - `requestCameraPermission()` - CAMERA

3. **HomeActivity.java**
   - `onRequestPermissionsResult()` - 모든 권한 콜백

#### 해결 방법
```java
// 기존 방식 (deprecated)
requireActivity().requestPermissions(permissions, REQUEST_CODE);

// 새로운 방식 (권장)
private final ActivityResultLauncher<String[]> requestPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), 
        result -> {
            // 권한 결과 처리
        });
```

---

### 3. requestLegacyExternalStorage

#### 문제점
- ⚠️ `android:requestLegacyExternalStorage="true"` (AndroidManifest.xml)
- ⚠️ Android 11+ (API 30+)에서 무시됨
- ⚠️ Android 10 (API 29)에서만 작동

#### 해결 방법
- Android 10 이하 지원 중단 시 제거 가능
- 또는 Scoped Storage로 마이그레이션

---

### 4. 권한 사용 이유 설명 부족

#### Play Store 정책
- 민감한 권한 사용 시 사용자에게 명확한 설명 필요
- 권한 요청 전에 rationale 제공 권장

#### 현재 상태
- ✅ POST_NOTIFICATIONS: `PermissionRationaleDialog` 사용
- ❌ CAMERA: 설명 없이 직접 요청
- ❌ LOCATION: 설명 없이 직접 요청
- ❌ STORAGE: 설명 없이 직접 요청

#### 권장 사항
모든 민감한 권한에 대해 rationale 다이얼로그 추가

---

## 🔍 추가 확인 사항

### 1. CAMERA 권한
- ✅ AndroidManifest에 선언됨
- ⚠️ 구식 요청 방식 사용
- ⚠️ 사용 이유 설명 없음

### 2. ACCESS_FINE_LOCATION 권한
- ❌ AndroidManifest에 선언되지 않음
- ⚠️ 코드에서 사용 중 (`DappBrowserFragment.java`)
- ⚠️ 구식 요청 방식 사용

### 3. VIBRATE 권한
- ✅ AndroidManifest에 선언됨
- ℹ️ 일반 권한 (런타임 요청 불필요)

### 4. USE_BIOMETRIC / USE_FINGERPRINT
- ✅ AndroidManifest에 선언됨
- ℹ️ 일반 권한 (런타임 요청 불필요)

---

## 📝 수정 우선순위

### 높음 (Play Store 제출 전 필수)
1. ✅ READ_EXTERNAL_STORAGE → READ_MEDIA_* 마이그레이션
2. ✅ ACCESS_FINE_LOCATION AndroidManifest 추가
3. ✅ 구식 권한 요청 방식 → ActivityResultLauncher 마이그레이션

### 중간 (권장)
4. ⚠️ 모든 민감한 권한에 rationale 다이얼로그 추가
5. ⚠️ requestLegacyExternalStorage 제거 또는 Scoped Storage 마이그레이션

### 낮음 (선택)
6. ℹ️ 권한 요청 타이밍 최적화
7. ℹ️ 권한 거부 시 대체 방법 제공

---

## 🎯 Play Store 준수성 체크리스트

- [x] targetSdk 35 (최신)
- [x] POST_NOTIFICATIONS 권한 최신 방식 사용
- [ ] READ_EXTERNAL_STORAGE → READ_MEDIA_* 마이그레이션
- [ ] 모든 권한 요청을 ActivityResultLauncher로 변경
- [ ] 모든 민감한 권한에 사용 이유 설명 제공
- [ ] AndroidManifest에 사용하는 모든 권한 선언
- [ ] 권한 사용 정당성 (Play Store 정책 준수)

---

## 📚 참고 자료

- [Android 13+ 권한 변경사항](https://developer.android.com/about/versions/13/behavior-changes-13#granular-media-permissions)
- [ActivityResultLauncher 가이드](https://developer.android.com/training/permissions/requesting)
- [Play Store 권한 정책](https://support.google.com/googleplay/android-developer/answer/9888170)

---

## 🔧 빠른 수정 가이드

### 1. AndroidManifest.xml 수정
```xml
<!-- 추가 필요 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 2. DappBrowserFragment.java 수정
- `checkReadPermission()` → ActivityResultLauncher 사용
- `requestGeoPermission()` → ActivityResultLauncher 사용
- `requestCameraPermission()` → ActivityResultLauncher 사용

### 3. QRScannerActivity.java 수정
- `requestCameraPermission()` → ActivityResultLauncher 사용

---

**생성일**: 2026-01-15
**검토 대상**: SetlOne Wallet App
**targetSdk**: 35 (Android 15)
