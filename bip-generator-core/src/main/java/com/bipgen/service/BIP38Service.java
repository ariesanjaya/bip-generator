package com.bipgen.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.base.Address;
import org.bitcoinj.base.Base58;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.crypto.generators.SCrypt;

import com.bipgen.model.BIP38DecryptResult;
import com.bipgen.model.BIP38EncryptResult;

public class BIP38Service {

    private final NetworkParameters params;
    private static final byte[] BIP38_PREFIX = new byte[]{0x01, 0x42};
    private static final byte[] BIP38_FLAGBYTE_COMPRESSED = new byte[]{(byte) 0xE0};
    private static final byte[] BIP38_FLAGBYTE_UNCOMPRESSED = new byte[]{(byte) 0xC0};

    public BIP38Service() {
        this.params = MainNetParams.get();
    }

    /**
     * Encrypt a WIF private key with a password (BIP38)
     */
    public BIP38EncryptResult encrypt(String privateKeyWif, String password) throws Exception {
        // Validate password
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }

        // Parse WIF private key
        byte[] decoded = Base58.decodeChecked(privateKeyWif);
        byte[] keyBytes = new byte[32];
        System.arraycopy(decoded, 1, keyBytes, 0, 32);

        // Determine if key is compressed based on WIF format
        boolean compressed = privateKeyWif.startsWith("K") || privateKeyWif.startsWith("L");

        // Create ECKey with correct compression flag
        ECKey key = ECKey.fromPrivate(keyBytes, compressed);

        // Get Bitcoin address for this key
        Address address = key.toAddress(ScriptType.P2PKH, params.network());
        String addressStr = address.toString();

        // Calculate address hash (first 4 bytes of double SHA256)
        byte[] addressHash = calculateAddressHash(addressStr);

        // Derive encryption key using scrypt
        byte[] derivedKey = SCrypt.generate(
            password.getBytes(StandardCharsets.UTF_8),
            addressHash,
            16384, // N
            8,     // r
            8,     // p
            64     // key length
        );

        byte[] derivedHalf1 = Arrays.copyOfRange(derivedKey, 0, 32);
        byte[] derivedHalf2 = Arrays.copyOfRange(derivedKey, 32, 64);

        // Get private key bytes (already 32 bytes)
        byte[] privateKeyBytes = key.getPrivKeyBytes();
        byte[] encryptedHalf1 = xor(Arrays.copyOfRange(privateKeyBytes, 0, 16), Arrays.copyOfRange(derivedHalf1, 0, 16));
        byte[] encryptedHalf2 = xor(Arrays.copyOfRange(privateKeyBytes, 16, 32), Arrays.copyOfRange(derivedHalf1, 16, 32));

        encryptedHalf1 = aesEncrypt(encryptedHalf1, derivedHalf2);
        encryptedHalf2 = aesEncrypt(encryptedHalf2, derivedHalf2);

        // Build BIP38 key
        ByteBuffer buffer = ByteBuffer.allocate(39);
        buffer.put(BIP38_PREFIX);
        buffer.put(compressed ? BIP38_FLAGBYTE_COMPRESSED : BIP38_FLAGBYTE_UNCOMPRESSED);
        buffer.put(addressHash);
        buffer.put(encryptedHalf1);
        buffer.put(encryptedHalf2);

        // Encode with checksum (prefix is already in the data)
        String encryptedKey = encodeBase58WithChecksum(buffer.array());

        return new BIP38EncryptResult(encryptedKey, compressed);
    }

    /**
     * Decrypt a BIP38 encrypted key with password
     */
    public BIP38DecryptResult decrypt(String encryptedKey, String password) throws Exception {
        // Validate BIP38 format
        if (!encryptedKey.startsWith("6P")) {
            throw new IllegalArgumentException("Invalid BIP38 key format. Must start with '6P'");
        }

        // Decode BIP38 key (custom decode to match our custom encode)
        byte[] decoded = decodeBase58WithChecksum(encryptedKey);

        if (decoded.length != 39) {
            throw new IllegalArgumentException("Invalid BIP38 key length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] prefix = new byte[2];
        buffer.get(prefix);

        if (!Arrays.equals(prefix, BIP38_PREFIX)) {
            throw new IllegalArgumentException("Invalid BIP38 prefix");
        }

        byte flagByte = buffer.get();
        boolean compressed = (flagByte == BIP38_FLAGBYTE_COMPRESSED[0]);

        byte[] addressHash = new byte[4];
        buffer.get(addressHash);

        byte[] encryptedHalf1 = new byte[16];
        byte[] encryptedHalf2 = new byte[16];
        buffer.get(encryptedHalf1);
        buffer.get(encryptedHalf2);

        // Derive decryption key using scrypt
        byte[] derivedKey = SCrypt.generate(
            password.getBytes(StandardCharsets.UTF_8),
            addressHash,
            16384,
            8,
            8,
            64
        );

        byte[] derivedHalf1 = Arrays.copyOfRange(derivedKey, 0, 32);
        byte[] derivedHalf2 = Arrays.copyOfRange(derivedKey, 32, 64);

        // Decrypt
        byte[] decryptedHalf1 = aesDecrypt(encryptedHalf1, derivedHalf2);
        byte[] decryptedHalf2 = aesDecrypt(encryptedHalf2, derivedHalf2);

        decryptedHalf1 = xor(decryptedHalf1, Arrays.copyOfRange(derivedHalf1, 0, 16));
        decryptedHalf2 = xor(decryptedHalf2, Arrays.copyOfRange(derivedHalf1, 16, 32));

        // Reconstruct private key
        byte[] privateKeyBytes = new byte[32];
        System.arraycopy(decryptedHalf1, 0, privateKeyBytes, 0, 16);
        System.arraycopy(decryptedHalf2, 0, privateKeyBytes, 16, 16);

        ECKey key = ECKey.fromPrivate(privateKeyBytes, compressed);

        // Verify address hash
        Address address = key.toAddress(ScriptType.P2PKH, params.network());
        String addressStr = address.toString();
        byte[] verifyHash = calculateAddressHash(addressStr);

        if (!Arrays.equals(addressHash, verifyHash)) {
            throw new IllegalArgumentException("Invalid password or corrupted key");
        }

        // Format private key as WIF (using stable method)
        ECKey resultKey = ECKey.fromPrivate(privateKeyBytes, compressed);
        String privateKeyWif = resultKey.getPrivateKeyEncoded(params.network()).toString();

        return new BIP38DecryptResult(privateKeyWif, addressStr, compressed);
    }

    /**
     * Calculate address hash (first 4 bytes of double SHA256)
     */
    private byte[] calculateAddressHash(String address) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash1 = digest.digest(address.getBytes(StandardCharsets.UTF_8));
        byte[] hash2 = digest.digest(hash1);
        return Arrays.copyOfRange(hash2, 0, 4);
    }

    /**
     * XOR two byte arrays
     */
    private byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    /**
     * AES encrypt
     */
    private byte[] aesEncrypt(byte[] data, byte[] key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOfRange(key, 0, 32), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * AES decrypt
     */
    private byte[] aesDecrypt(byte[] data, byte[] key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOfRange(key, 0, 32), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * Encode data with Base58Check (adds checksum but no version byte)
     * Used for BIP38 where version bytes are already in the data
     */
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

        // Encode with Base58
        return Base58.encode(dataWithChecksum);
    }

    /**
     * Decode Base58Check data (verifies and removes checksum)
     * Used for BIP38 where version bytes are in the data
     */
    private byte[] decodeBase58WithChecksum(String encoded) throws Exception {
        // Decode from Base58
        byte[] decoded = Base58.decode(encoded);

        if (decoded.length < 4) {
            throw new IllegalArgumentException("Invalid Base58 data");
        }

        // Split data and checksum
        int dataLength = decoded.length - 4;
        byte[] data = Arrays.copyOfRange(decoded, 0, dataLength);
        byte[] checksumReceived = Arrays.copyOfRange(decoded, dataLength, decoded.length);

        // Calculate expected checksum
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash1 = digest.digest(data);
        byte[] hash2 = digest.digest(hash1);
        byte[] checksumExpected = Arrays.copyOfRange(hash2, 0, 4);

        // Verify checksum
        if (!Arrays.equals(checksumReceived, checksumExpected)) {
            throw new IllegalArgumentException("Invalid checksum");
        }

        return data;
    }

}
