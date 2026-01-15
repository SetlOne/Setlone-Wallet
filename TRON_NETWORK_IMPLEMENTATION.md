# TRON 네트워크 구현 가이드

## 중요 사항

**TRON은 EVM 호환 네트워크가 아닙니다.** 따라서 이더리움 계열 네트워크와는 완전히 다른 방식으로 처리해야 합니다.

## 1. 네트워크 식별

### Chain ID vs CoinType

- **Chain ID (메타데이터용)**: `728126428` (10진수) / `0x2b6653dc` (16진수)
  - 네트워크 리스트 표시 및 UI에서 사용
  - **주의**: 실제 트랜잭션 서명에는 사용되지 않음

- **CoinType (실제 사용)**: `195` (BIP44)
  - wallet-core에서 실제 주소 생성 및 서명에 사용
  - Derivation Path: `m/44'/195'/0'/0/0`

### 코드에서 사용

```java
// Chain ID로 TRON 체크
if (TronUtils.isTronChain(chainId)) {
    // TRON 전용 로직
}

// wallet-core에서 주소 생성
PrivateKey pk = wallet.getKeyForCoin(CoinType.TRON);
String tronAddress = CoinType.TRON.deriveAddress(pk);
```

## 2. 네트워크 설정

### RPC URL (실제로는 HTTP API)

TRON은 JSON-RPC를 사용하지 않으므로, `rpcUrls` 필드에 저장된 값은 실제로는 HTTP API 엔드포인트입니다.

**현재 설정된 엔드포인트:**
- `https://api.trongrid.io` (TronGrid 공식 API)
- `https://tron.drpc.org` (dRPC 엔드포인트)
- `https://tron-rpc.publicnode.com` (PublicNode 엔드포인트)

**주의**: Web3j를 사용할 수 없습니다. 모든 TRON 작업은 `TronService`를 통해 HTTP API로 직접 처리해야 합니다.

## 3. 주소 관리

### 주소 형식

- **EVM 주소**: `0x`로 시작, 42자리 (20 bytes hex)
- **TRON 주소**: `T`로 시작, 34자리 Base58 문자열

### 주소 검증

```java
// TRON 주소 검증
if (TronUtils.isValidTronAddress(address)) {
    // TRON 주소 처리
}

// 통합 주소 검증 (EVM + TRON)
if (Utils.isAddressValid(address)) {
    // 자동으로 TRON 또는 EVM 주소 구분
}
```

### 주소 포맷팅

```java
// TRON 주소도 자동으로 포맷팅됨
String formatted = Utils.formatAddress(tronAddress);
// 예: "T9yD1...abcd"
```

## 4. 트랜잭션 처리

### EVM vs TRON

| 항목 | EVM 네트워크 | TRON |
|------|-------------|------|
| 트랜잭션 형식 | RLP 인코딩 | Protobuf |
| 서명 방식 | ECDSA (secp256k1) | ECDSA (secp256k1) |
| Chain ID 사용 | 서명에 포함 | 사용 안 함 (RefBlock 사용) |
| Nonce | 사용 | 사용 안 함 |
| Gas | ETH/BNB 등 | Energy & Bandwidth |
| RPC | JSON-RPC | HTTP API (gRPC도 가능) |

### 트랜잭션 서명

**EVM 네트워크:**
```java
// TransactionRepository.signTransaction() 사용
// Web3j를 통한 JSON-RPC 호출
```

**TRON 네트워크:**
```java
// TronService를 통한 HTTP API 호출
// Protobuf 형식으로 트랜잭션 생성
// RefBlockBytes, RefBlockHash 필요 (최신 블록 정보)
```

## 5. Web3j 사용 불가

TRON 네트워크에서는 Web3j를 사용할 수 없습니다. 다음 메서드들은 TRON 체인 ID가 들어오면 예외를 발생시킵니다:

- `TokenRepository.getWeb3jService(chainId)`
- `TokenRepository.getWeb3jServiceForEvents(chainId)`
- `TokenRepository.buildWeb3jClient(networkInfo)`
- `TokenRepository.getService(chainId)`

**대신 사용:**
- `TronService`를 통해 HTTP API 직접 호출

## 6. 지갑 주소 저장

### 현재 구조의 한계

현재 `Wallet` 엔티티는 단일 `address` 필드만 가지고 있어, 네트워크별 주소를 구분할 수 없습니다.

**문제점:**
- 같은 니모닉에서 ETH 주소(`0x...`)와 TRON 주소(`T...`)를 모두 생성할 수 있지만, 현재는 하나만 저장됨
- 네트워크별 주소 매핑이 없음

**해결 방안 (향후 구현):**
1. 지갑 생성 시 모든 네트워크 주소 생성 및 저장
2. 네트워크별 주소 매핑 구조 추가 (`Map<Long, String>`)
3. 또는 TRON 전용 지갑 엔티티 생성

## 7. 네트워크 추가 UI

### 현재 상태

네트워크 추가 UI(`AddCustomRPCNetworkActivity`)는 EVM 네트워크를 위한 구조입니다:
- RPC URL 입력 필수
- Chain ID 입력
- Symbol 입력

### TRON 네트워크 추가 시

TRON은 이미 built-in 네트워크로 추가되어 있으므로, 사용자가 수동으로 추가하려는 경우:
- Chain ID 728126428 입력 시 경고 표시 (이미 존재)
- 또는 TRON 전용 추가 UI 제공

## 8. 구현된 기능

✅ **완료된 작업:**
1. TRON 네트워크 설정 (Chain ID 728126428)
2. TRON 주소 생성 (`TronAddressGenerator`, `KeyService`)
3. TRON 주소 검증 (`TronUtils.isValidTronAddress()`)
4. 통합 주소 검증 (`Utils.isAddressValid()` - TRON + EVM)
5. 주소 포맷팅 (`Utils.formatAddress()` - TRON + EVM)
6. Web3j 사용 방지 (TRON 체크 추가)
7. 트랜잭션 서명 방지 (TRON 체크 추가)
8. TRON 로고 및 색상 추가

## 9. 미구현 기능

❌ **아직 구현되지 않은 기능:**
1. TRON 트랜잭션 서명 (Protobuf 형식)
2. TRON 트랜잭션 브로드캐스트
3. TRON 잔액 조회 (TronService에 구현되어 있으나 통합 필요)
4. TRON 토큰(TRC20) 지원
5. Energy & Bandwidth 계산
6. 네트워크별 지갑 주소 매핑

## 10. 참고 자료

- **wallet-core**: CoinType.TRON (195) 사용
- **TronGrid API**: https://developers.tron.network/
- **TronScan**: https://tronscan.org/
- **TRON 공식 문서**: https://developers.tron.network/docs

## 11. 체크리스트

- [x] Chain ID 728126428 확인
- [x] CoinType.TRON (195) 사용 확인
- [x] TRON 주소 생성 구현
- [x] TRON 주소 검증 구현
- [x] Web3j 사용 방지
- [x] 트랜잭션 서명 방지
- [ ] TRON 트랜잭션 서명 구현
- [ ] TRON 트랜잭션 브로드캐스트 구현
- [ ] 네트워크별 주소 매핑 구조 추가
