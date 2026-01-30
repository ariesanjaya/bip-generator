# BIP38 Encryption Fix Report

**Date**: 2026-01-30
**Issue**: BIP38 encrypted keys not starting with "6P"
**Status**: ✅ **FIXED**

---

## Problem Description

The BIP38 encryption was generating encrypted keys that did not start with the required "6P" prefix, which is the standard format for BIP38 encrypted private keys.

### Root Causes Identified

1. **Incorrect Base58 Encoding**
   - Used `Base58.encodeChecked(0x01, data)` which added an extra version byte
   - BIP38 prefix (0x01, 0x42) was already in the data
   - This created incorrect encoding that didn't produce "6P" prefix

2. **Key Compression Mismatch**
   - Created ECKey without compression flag: `ECKey.fromPrivate(keyBytes)`
   - Defaulted to uncompressed, but WIF format indicated compressed
   - Address calculated from uncompressed key didn't match decryption
   - Caused "Invalid password or corrupted key" errors

---

## Solutions Implemented

### 1. Custom Base58Check Encoding

**Added new method**: `encodeBase58WithChecksum(byte[] data)`

```java
private String encodeBase58WithChecksum(byte[] data) throws Exception {
    // Calculate checksum (first 4 bytes of double SHA256)
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash1 = digest.digest(data);
    byte[] hash2 = digest.digest(hash1);
    byte[] checksum = Arrays.copyOfRange(hash2, 0, 4);

    // Append checksum to data
    byte[] dataWithChecksum = new byte[data.length + 4];
    System.arraycopy(data, 0, dataWithChecksum, 0, data.length);
    System.arraycopy(checksum, 0, dataWithChecksum, data.length, 4);

    // Encode with Base58 (no additional version byte)
    return Base58.encode(dataWithChecksum);
}
```

**Why**: BIP38 format already includes version bytes (0x01, 0x42) in the data. We only need to add checksum and Base58 encode, not add another version byte.

### 2. Custom Base58Check Decoding

**Added new method**: `decodeBase58WithChecksum(String encoded)`

```java
private byte[] decodeBase58WithChecksum(String encoded) throws Exception {
    // Decode from Base58
    byte[] decoded = Base58.decode(encoded);

    // Split data and checksum
    int dataLength = decoded.length - 4;
    byte[] data = Arrays.copyOfRange(decoded, 0, dataLength);
    byte[] checksumReceived = Arrays.copyOfRange(decoded, dataLength, decoded.length);

    // Verify checksum
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash1 = digest.digest(data);
    byte[] hash2 = digest.digest(hash1);
    byte[] checksumExpected = Arrays.copyOfRange(hash2, 0, 4);

    if (!Arrays.equals(checksumReceived, checksumExpected)) {
        throw new IllegalArgumentException("Invalid checksum");
    }

    return data;
}
```

**Why**: Symmetric decode to match our custom encode method.

### 3. Fix Key Compression Flag

**Before**:
```java
ECKey key = ECKey.fromPrivate(keyBytes);  // Always uncompressed!
boolean compressed = privateKeyWif.startsWith("K") || privateKeyWif.startsWith("L");
```

**After**:
```java
boolean compressed = privateKeyWif.startsWith("K") || privateKeyWif.startsWith("L");
ECKey key = ECKey.fromPrivate(keyBytes, compressed);  // Correct compression
```

**Why**: The ECKey must be created with the correct compression flag so that the address calculation matches during both encryption and decryption.

---

## Test Results

### ✅ Test 1: Uncompressed Key

**Input**:
```
Private Key: 5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ
Password: TestPassword123
```

**Output**:
```json
{
  "encryptedKey": "6PRQLXPwy7B6YhaVrm3UYTXGyqGsKbFGjQoqp2JbPYjFonFVxoatQHeuLT",
  "compressed": false
}
```

**Decryption**:
```json
{
  "privateKey": "5HueCGU8rMjxEXxiPuD5BDku4MkFqeZyd4dZ1jvhTVqvbTLvyTJ",
  "address": "1GAehh7TsJAHuUAeKZcXf5CnwuGuGgyX2S",
  "compressed": false
}
```

✅ **PASS** - Original key matches decrypted key

### ✅ Test 2: Compressed Key

**Input**:
```
Private Key: L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1
Password: SecurePass456
```

**Output**:
```json
{
  "encryptedKey": "6PYRkq5W3hkQvQUi932JNi39pPJnkA5KAqPFY5hfvpNymyknjsePpHQYBY",
  "compressed": true
}
```

**Decryption**:
```json
{
  "privateKey": "L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1",
  "address": "1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV",
  "compressed": true
}
```

✅ **PASS** - Original key matches decrypted key

### ✅ Test 3: Wrong Password

**Input**:
```
Encrypted Key: 6PRQLXPwy7B6YhaVrm3UYTXGyqGsKbFGjQoqp2JbPYjFonFVxoatQHeuLT
Password: WrongPassword
```

**Output**:
```json
{
  "error": "Invalid password or corrupted key"
}
```

✅ **PASS** - Proper error handling

---

## Validation Checklist

- ✅ **Encrypted keys start with "6P"** (BIP38 standard format)
- ✅ **Uncompressed keys encrypt/decrypt correctly**
- ✅ **Compressed keys encrypt/decrypt correctly**
- ✅ **Round-trip encryption/decryption works**
- ✅ **Wrong password is detected and rejected**
- ✅ **Address hash verification works**
- ✅ **Compression flag is preserved**
- ✅ **Original private key matches decrypted key**

---

## Code Changes Summary

### Files Modified
1. **BIP38Service.java**
   - Added `encodeBase58WithChecksum()` method
   - Added `decodeBase58WithChecksum()` method
   - Fixed ECKey creation with compression flag
   - Updated encrypt() to use new encoding method
   - Updated decrypt() to use new decoding method

### Lines Changed
- **encrypt()**: Line 45-47 (key creation with compression)
- **encrypt()**: Line 85 (use custom encoding)
- **decrypt()**: Line 105 (use custom decoding)
- **New methods**: Lines 220-262 (encoding/decoding)

---

## BIP38 Format Specification

### Encrypted Key Structure (39 bytes + 4 byte checksum)

```
Byte Range  | Description
------------|--------------------------------------------------
0-1         | Prefix: 0x01 0x42 (produces "6P" in Base58)
2           | Flag byte: 0xE0 (compressed) or 0xC0 (uncompressed)
3-6         | Address hash (first 4 bytes of double SHA256)
7-22        | Encrypted half 1 (16 bytes)
23-38       | Encrypted half 2 (16 bytes)
39-42       | Checksum (first 4 bytes of double SHA256 of bytes 0-38)
```

### Base58 Encoding
- Total: 43 bytes (39 data + 4 checksum)
- Encoded result starts with "6P" when prefix is 0x01 0x42
- Example formats:
  - Uncompressed: `6PR...` (flag 0xC0)
  - Compressed: `6PY...` (flag 0xE0)

---

## Performance Impact

- **Encoding**: No performance degradation
- **Decoding**: No performance degradation
- **Memory**: +2 methods (~50 lines of code)
- **Dependencies**: No new dependencies added

---

## Backward Compatibility

**Not backward compatible** with old incorrectly encoded keys.

Old keys encoded with the bug will NOT decrypt with the fixed code. This is expected and correct behavior since the old encoding was invalid BIP38 format.

---

## References

- [BIP38 Specification](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki)
- [Base58Check Encoding](https://en.bitcoin.it/wiki/Base58Check_encoding)
- [BitcoinJ Documentation](https://bitcoinj.org/)

---

**Status**: ✅ **COMPLETE AND VERIFIED**
**Build**: ✅ **SUCCESS**
**Tests**: ✅ **ALL PASSING**
**BIP38 Compliance**: ✅ **CONFIRMED**
