# 빌드 오류 로그

## 빌드 일시
최근 빌드 시 발생한 오류들

---

## 🔴 치명적 오류 (빌드 실패)

### 1. 파일명과 클래스명 불일치 오류 (4개)
**오류 메시지:**
```
error: class SetlOneGlideModule is public, should be declared in a file named SetlOneGlideModule.java
error: class SetlOneFirebaseMessagingService is public, should be declared in a file named SetlOneFirebaseMessagingService.java
error: class SetlOneNotificationService is public, should be declared in a file named SetlOneNotificationService.java
error: class SetlOneService is public, should be declared in a file named SetlOneService.java
```

**원인:**
- 클래스명은 `SetlOne*`로 변경되었지만 파일명은 여전히 `AlphaWallet*`로 되어 있음
- Java에서는 public 클래스명과 파일명이 일치해야 함

**해결 방법:**
- `AlphaWalletGlideModule.java` → `SetlOneGlideModule.java`로 파일명 변경
- `AlphaWalletFirebaseMessagingService.java` → `SetlOneFirebaseMessagingService.java`로 파일명 변경
- `AlphaWalletNotificationService.java` → `SetlOneNotificationService.java`로 파일명 변경
- `AlphaWalletService.java` → `SetlOneService.java`로 파일명 변경
- AndroidManifest.xml에서 참조하는 경우 경로도 업데이트 필요

---

### 2. 리소스 파일 참조 오류
**오류 메시지:**
```
error: cannot find symbol
  symbol:   variable setlone_min
  location: class raw
```

**원인:**
- `JsInjectorClient.java:76`에서 `R.raw.setlone_min`을 참조하지만
- 실제 파일명은 `alphawallet_min.js`임

**해결 방법:**
- `app/src/main/res/raw/alphawallet_min.js` → `setlone_min.js`로 파일명 변경
- 또는 `JsInjectorClient.java`에서 `R.raw.alphawallet_min`으로 참조 변경

---

## ⚠️ 경고 (빌드는 계속되지만 수정 권장)

### 3. strings.xml 포맷 경고
**경고 메시지:**
```
Multiple substitutions specified in non-positional format of string resource string/error_eip712_incompatible_network. 
Did you mean to add the formatted="false" attribute?
```

**영향받는 파일들:**
- `values/strings.xml`
- `values-zh/strings.xml`
- `values-id/strings.xml`
- `values-vi/strings.xml`
- `values-es/strings.xml`
- `values-my/strings.xml`
- `values-fr/strings.xml`

**영향받는 리소스:**
- `error_eip712_incompatible_network`
- `notification_message_incoming_token`
- `notification_message_incoming_token_with_recipient`
- `total_cost_for_x_tickets`
- `link_valid_until`
- `set_price_subtext_abr`
- `developer_override_warning`

**해결 방법:**
- 해당 string 리소스에 `formatted="false"` 속성 추가

---

### 4. AndroidManifest.xml 경고
**경고 메시지:**
```
application@android:name was tagged at AndroidManifest.xml:19 to replace other declarations but no other declaration present
application@android:theme was tagged at AndroidManifest.xml:19 to replace other declarations but no other declaration present
application@android:allowBackup was tagged at AndroidManifest.xml:19 to replace other declarations but no other declaration present
uses-permission#com.google.android.gms.permission.AD_ID was tagged at AndroidManifest.xml:0 to remove other declarations but no other declaration present
```

**원인:**
- `app/src/debug/AndroidManifest.xml`에서 override 태그가 있지만 메인 매니페스트에 해당 속성이 없음

**해결 방법:**
- 디버그 매니페스트의 불필요한 override 태그 제거 또는 메인 매니페스트에 해당 속성 추가

---

## 📝 남아있는 AlphaWallet 참조 (빌드에는 영향 없지만 리브랜딩 필요)

### 5. 코드 내 상수 및 변수명
**위치:**
- `app/src/main/java/com/setlone/app/util/Utils.java`:
  - `ALPHAWALLET_REPO_NAME`
  - `ALPHAWALLET_ICON_REPO`
  - `DAPP_PREFIX_ALPHAWALLET`
- `app/src/main/java/com/setlone/app/viewmodel/HomeViewModel.java`:
  - `ALPHAWALLET_DIR` (값은 "SetlOne"로 이미 변경됨)
  - `setlOneNotificationService` (변수명)
- `app/src/main/java/com/setlone/app/ui/HomeActivity.java`:
  - `AWalletAlertDialog`
  - `AWalletConfirmationDialog`

### 6. strings.xml 텍스트
**위치:**
- `values/strings.xml`: "Why does aWallet use Ethereum?"
- `values-vi/strings.xml`: "Tại sao aWallet sử dụng Ethereum?"
- `values-id/strings.xml`: "Mengapa aWallet menggunakan Ethereum?"

### 7. SetlOneService.java 내부 URL
**위치:**
- `app/src/main/java/com/setlone/app/service/SetlOneService.java:50`:
  - `XML_VERIFIER_ENDPOINT = "https://aw.app/api/v1/verifyXMLDSig"`
  - `aw.app` 도메인은 AlphaWallet 소유일 수 있음

---

## ✅ 수정 완료 항목
- 패키지명 변경: `com.alphawallet` → `com.setlone`
- 클래스명 변경: `AlphaWallet*` → `SetlOne*` (일부)
- AndroidManifest.xml 서비스 참조 업데이트
- 빌드 설정 파일 업데이트

---

## 🔧 다음 단계
1. ✅ 파일명 변경 (4개 파일) - 완료
2. ✅ 리소스 파일명 변경 또는 참조 수정 - 완료
3. strings.xml 포맷 속성 추가 (선택사항)
4. 남아있는 AlphaWallet 참조 정리 (선택사항)

---

## ✅ 수정 완료 및 빌드 성공

**수정 일시:** 최근 빌드

**수정 내용:**
1. ✅ `AlphaWalletGlideModule.java` → `SetlOneGlideModule.java` 파일명 변경
2. ✅ `AlphaWalletFirebaseMessagingService.java` → `SetlOneFirebaseMessagingService.java` 파일명 변경
3. ✅ `AlphaWalletNotificationService.java` → `SetlOneNotificationService.java` 파일명 변경
4. ✅ `AlphaWalletService.java` → `SetlOneService.java` 파일명 변경
5. ✅ `alphawallet_min.js` → `setlone_min.js` 리소스 파일 복사 (JsInjectorClient.java에서 참조)

**빌드 결과:**
```
BUILD SUCCESSFUL in 1m 40s
```

모든 치명적 오류가 해결되어 빌드가 성공적으로 완료되었습니다.
