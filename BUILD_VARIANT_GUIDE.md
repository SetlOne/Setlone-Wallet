# Build Variant 선택 가이드 (noAnalyticsDebug)

## 🔍 Build Variants 탭이 보이지 않을 때 해결 방법

### 방법 1: 메뉴에서 Build Variants 열기 (가장 확실한 방법)

1. **View → Tool Windows → Build Variants**
   - 또는 단축키: 없음 (메뉴 사용)
2. 왼쪽 하단에 **Build Variants** 창이 나타납니다
3. **app** 모듈을 찾아서 드롭다운 클릭
4. **noAnalyticsDebug** 선택

### 방법 2: Build Variants 탭이 숨겨진 경우

1. **View → Tool Windows** 메뉴 확인
2. **Build Variants** 항목이 체크되어 있는지 확인
3. 체크되어 있으면 왼쪽 하단을 확인
4. 여러 탭이 겹쳐 있을 수 있으므로 탭 이름을 클릭하여 전환

### 방법 3: Run Configuration에서 선택

1. 상단 툴바의 **Run/Debug Configurations** 드롭다운 클릭
2. **Edit Configurations...** 선택
3. 왼쪽에서 **app** 선택 (없으면 + 버튼으로 추가)
4. **General** 탭에서:
   - **Build variant**: `noAnalyticsDebug` 선택
5. **Apply** → **OK**
6. 이제 Run/Debug 버튼을 클릭하면 noAnalyticsDebug로 실행됩니다

### 방법 4: Gradle Tasks에서 직접 실행

1. 오른쪽 **Gradle** 탭 열기 (없으면 **View → Tool Windows → Gradle**)
2. **app → Tasks → install** 폴더 확장
3. **installNoAnalyticsDebug** 더블클릭
4. 에뮬레이터에 자동으로 설치됩니다

### 방법 5: 터미널에서 실행

Android Studio 하단의 **Terminal** 탭에서:

```powershell
# noAnalyticsDebug 빌드 및 설치
.\gradlew.bat installNoAnalyticsDebug

# 또는 디버그 APK만 빌드
.\gradlew.bat assembleNoAnalyticsDebug
```

---

## ✅ 확인 방법

Build Variant가 제대로 선택되었는지 확인:

1. **Build → Select Build Variant...** 메뉴 클릭
2. 또는 상단 툴바의 **Build Variant** 표시 확인 (있는 경우)
3. Run Configuration에서 확인

---

## 🎯 빠른 해결 (권장 순서)

### Step 1: 메뉴로 열기
```
View → Tool Windows → Build Variants
```

### Step 2: app 모듈 찾기
- Build Variants 창에서 **app** 모듈 찾기

### Step 3: 드롭다운 클릭
- **app** 옆의 드롭다운 클릭 (기본적으로 `analyticsDebug`로 표시됨)

### Step 4: noAnalyticsDebug 선택
- 드롭다운에서 **noAnalyticsDebug** 선택

### Step 5: 확인
- 선택 후 자동으로 Gradle 동기화가 시작됩니다
- 완료되면 Run 버튼을 클릭하면 noAnalyticsDebug로 실행됩니다

---

## 🔧 여전히 안 보이는 경우

### Gradle 동기화
1. **File → Sync Project with Gradle Files**
2. 동기화 완료 후 다시 시도

### 프로젝트 구조 확인
1. **File → Project Structure**
2. **Modules** 탭에서 **app** 모듈 확인
3. **Flavors** 섹션에 `analytics`와 `noAnalytics`가 있는지 확인

### 캐시 무효화
1. **File → Invalidate Caches / Restart**
2. **Invalidate and Restart** 클릭
3. Android Studio 재시작 후 다시 시도

---

## 📝 참고: 사용 가능한 Build Variants

프로젝트에는 다음 Build Variants가 있습니다:

- ✅ **noAnalyticsDebug** ← 원하는 것
- **analyticsDebug** (기본값)
- **noAnalyticsRelease** (서명 필요)
- **analyticsRelease** (서명 필요)

---

## 💡 팁

1. **Build Variants 창 위치 변경**: 창을 드래그하여 원하는 위치로 이동 가능
2. **고정**: 창 제목을 우클릭하여 **Docked** 또는 **Pinned** 설정 가능
3. **단축키 설정**: **File → Settings → Keymap**에서 Build Variants에 단축키 설정 가능
