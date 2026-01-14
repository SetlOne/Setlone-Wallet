# Android Studio Logcat 필터링 가이드

## 📋 기본 필터 설정

### 1. **최소한의 필터 (추천)**
```
package:com.setlone.wallet level:error
```
- **설명**: 앱의 에러 레벨만 표시
- **장점**: 핵심 오류만 빠르게 확인 가능
- **단점**: 경고나 정보성 로그는 제외됨

### 2. **권장 필터 (문제 발생 시)**
```
package:com.setlone.wallet level:error,assert
```
- **설명**: 에러와 Assert 레벨 표시
- **사용 시점**: 앱 크래시나 심각한 오류 발생 시

### 3. **상세 필터 (디버깅 시)**
```
package:com.setlone.wallet level:error,assert,warn
```
- **설명**: 에러, Assert, 경고 레벨 표시
- **사용 시점**: 문제 원인을 더 자세히 분석할 때

## 🔍 키워드 기반 필터

### 4. **크래시/예외 필터**
```
package:com.setlone.wallet (FATAL OR AndroidRuntime OR Exception OR Error OR crash)
```
- **설명**: 크래시 및 예외 관련 로그만 표시
- **사용 시점**: 앱이 크래시할 때

### 5. **지갑 관련 오류 필터**
```
package:com.setlone.wallet level:error (wallet OR Wallet OR WalletsViewModel OR fetch OR database)
```
- **설명**: 지갑 및 데이터베이스 관련 오류
- **사용 시점**: 지갑 생성/불러오기 오류 시

### 6. **WalletConnect 오류 필터**
```
package:com.setlone.wallet level:error (WalletConnect OR walletconnect OR WC)
```
- **설명**: WalletConnect 관련 오류만
- **사용 시점**: WalletConnect 연결 오류 시

## 📊 상황별 필터 가이드

### 상황 1: 앱이 시작조차 안 될 때
```
package:com.setlone.wallet level:error,assert
```
+ **추가 확인**: 프로세스 시작 로그 (`PROCESS STARTED`)

### 상황 2: 특정 기능에서 오류 발생 시
```
package:com.setlone.wallet level:error,assert,warn (기능명)
```
예시: `package:com.setlone.wallet level:error (wallet OR token OR transaction)`

### 상황 3: 빌드는 성공했지만 런타임 오류 시
```
package:com.setlone.wallet level:error (NoClassDefFoundError OR ClassNotFoundException OR IllegalStateException OR NullPointerException)
```

### 상황 4: 네트워크 관련 오류 시
```
package:com.setlone.wallet level:error (network OR Network OR HTTP OR connection)
```

## 🎯 추천: 평소 사용할 필터

### **기본 필터 (일상 사용) - WebView 경고 제외**
```
package:com.setlone.wallet level:error,assert
```
- **설명**: 자신의 앱의 에러만 표시 (WebView 시스템 경고 자동 제외)
- **장점**: WebView tile memory 경고 등 시스템 경고가 보이지 않음

### **문제 발생 시 (디버깅)**
```
package:com.setlone.wallet level:error,assert,warn
```

## 🚫 WebView 경고 제외 방법

### 방법 1: 패키지 필터 사용 (권장)
```
package:com.setlone.wallet level:error
```
- **가장 간단하고 효과적**: 자신의 앱 로그만 표시
- WebView 시스템 경고는 자동으로 제외됨
- 현재 `level:error`만 사용 중이라면 이 방법을 추천합니다

### 방법 2: 태그 제외 (고급)
```
level:error -tag:chromium -tag:webview_zygote
```
- **설명**: chromium, webview_zygote 태그 제외
- **단점**: 패키지 필터가 없어서 다른 앱의 에러도 표시됨

### 방법 3: 복합 필터 (상세 제어)
```
package:com.setlone.wallet level:error -tag:chromium -tag:webview_zygote
```
- **설명**: 자신의 앱 + WebView 태그 제외
- **사용 시점**: 패키지 필터만으로도 충분하지만, 추가로 제외하고 싶을 때

## 📝 Logcat 사용 방법

### Android Studio에서:
1. **하단 패널** → **Logcat** 탭 클릭
2. **필터 입력란**에 위 필터 중 하나 입력
3. **Enter** 키로 적용

### 필터 저장하기:
1. 필터 입력 후 **Save Filter** 버튼 클릭
2. 필터 이름 지정 (예: "SetlOne Errors")
3. 저장된 필터는 드롭다운에서 선택 가능

## ⚠️ 주의사항

1. **Error 레벨만으로도 충분**: 대부분의 경우 `level:error`만으로도 충분합니다.
2. **너무 많은 필터 피하기**: 너무 복잡한 필터는 오히려 중요한 로그를 놓칠 수 있습니다.
3. **타임스탬프 확인**: 오류 발생 시각을 기록해두면 추적이 쉬워집니다.

## 🔧 추가 팁

### 로그를 파일로 저장:
1. Logcat에서 **Export to File** 클릭
2. 파일명 지정 (예: `error.log`)
3. 저장된 파일을 공유하면 분석이 쉬워집니다.

### 특정 시간대 로그 확인:
1. Logcat 상단의 **시간 필터** 사용
2. 또는 로그 파일을 텍스트 에디터에서 열어 검색

---

**💡 요약**: 평소에는 `package:com.setlone.wallet level:error` 필터만 사용하시면 됩니다. 문제 발생 시 `level:error,assert,warn`으로 확장하세요!
