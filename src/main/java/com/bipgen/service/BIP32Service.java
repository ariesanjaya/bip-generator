package com.bipgen.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.bitcoinj.base.Address;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BIP32Service {

    private final NetworkParameters params;

    public BIP32Service() {
        this.params = MainNetParams.get();
    }

    /**
     * Generate a new BIP32 master key with mnemonic
     * @param customMnemonic Optional custom mnemonic phrase (12 or 24 words)
     * @param wordCount Number of words (12 or 24)
     * @param passphrase Optional BIP39 passphrase (empty string if not used)
     */
    public JsonObject generateMasterKey(String customMnemonic, int wordCount, String passphrase) throws Exception {
        DeterministicSeed seed;

        // Use empty string if passphrase is null
        String actualPassphrase = (passphrase != null && !passphrase.isEmpty()) ? passphrase : "";

        if (customMnemonic != null && !customMnemonic.trim().isEmpty()) {
            // Use custom mnemonic
            List<String> words = Arrays.asList(customMnemonic.trim().split("\\s+"));
            seed = DeterministicSeed.ofMnemonic(words, actualPassphrase, Instant.now());
        } else {
            // Generate new mnemonic using MnemonicCode
            SecureRandom random = new SecureRandom();
            int entropyBits = (wordCount == 24) ? 256 : 128;
            byte[] entropy = new byte[entropyBits / 8];
            random.nextBytes(entropy);

            List<String> words = MnemonicCode.INSTANCE.toMnemonic(entropy);
            seed = DeterministicSeed.ofMnemonic(words, actualPassphrase, Instant.now());
        }

        // Create key chain from seed
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();
        DeterministicKey masterKey = keyChain.getWatchingKey();

        // Build response
        JsonObject response = new JsonObject();
        response.put("mnemonic", String.join(" ", seed.getMnemonicCode()));
        // Serialize keys (using original methods as they are stable)
        response.put("masterPrivateKey", masterKey.serializePrivB58(params.network()));
        response.put("masterPublicKey", masterKey.serializePubB58(params.network()));
        response.put("seed", seed.toHexString());
        response.put("hasPassphrase", !actualPassphrase.isEmpty());

        return response;
    }

    /**
     * Derive child key from master key using BIP32 path
     */
    public JsonObject deriveChildKey(String masterKeyStr, String path) throws Exception {
        // Parse master key (using stable method)
        DeterministicKey masterKey = DeterministicKey.deserializeB58(masterKeyStr, params.network());

        // Parse derivation path (e.g., "m/44'/0'/0'/0/0")
        DeterministicKey derivedKey = deriveFromPath(masterKey, path);

        // Get address
        Address address = derivedKey.toAddress(ScriptType.P2PKH, params.network());
        String addressStr = address.toString();

        // Build response
        JsonObject response = new JsonObject();
        response.put("path", path);
        // Format private key as WIF
        response.put("privateKey", formatPrivateKeyAsWIF(derivedKey.getPrivKeyBytes(), true));
        response.put("publicKey", derivedKey.getPublicKeyAsHex());
        response.put("address", addressStr);

        return response;
    }

    /**
     * Generate multiple addresses from a derivation path pattern
     */
    public JsonObject deriveMultipleAddresses(String masterKeyStr, String pathPattern, int count) throws Exception {
        // Parse master key (using stable method)
        DeterministicKey masterKey = DeterministicKey.deserializeB58(masterKeyStr, params.network());

        JsonArray addresses = new JsonArray();

        for (int i = 0; i < count; i++) {
            String path = pathPattern.replace("*", String.valueOf(i));
            DeterministicKey derivedKey = deriveFromPath(masterKey, path);

            Address address = derivedKey.toAddress(ScriptType.P2PKH, params.network());

            JsonObject addressInfo = new JsonObject();
            addressInfo.put("index", i);
            addressInfo.put("path", path);
            addressInfo.put("address", address.toString());
            // Format private key as WIF
            addressInfo.put("privateKey", formatPrivateKeyAsWIF(derivedKey.getPrivKeyBytes(), true));
            addressInfo.put("publicKey", derivedKey.getPublicKeyAsHex());

            addresses.add(addressInfo);
        }

        JsonObject response = new JsonObject();
        response.put("addresses", addresses);
        response.put("count", count);

        return response;
    }

    /**
     * Derive key from BIP32 path string
     */
    private DeterministicKey deriveFromPath(DeterministicKey masterKey, String path) throws Exception {
        // Remove 'm/' or 'M/' prefix if present
        String cleanPath = path.replaceFirst("^[mM]/", "");

        if (cleanPath.isEmpty()) {
            return masterKey;
        }

        String[] parts = cleanPath.split("/");
        DeterministicKey currentKey = masterKey;

        for (String part : parts) {
            boolean hardened = part.endsWith("'") || part.endsWith("h");
            int index = Integer.parseInt(part.replaceAll("['^h]", ""));

            if (hardened) {
                currentKey = HDKeyDerivation.deriveChildKey(currentKey, new ChildNumber(index, true));
            } else {
                currentKey = HDKeyDerivation.deriveChildKey(currentKey, new ChildNumber(index, false));
            }
        }

        return currentKey;
    }

    /**
     * Format private key bytes as WIF (Wallet Import Format)
     * This is a custom implementation to avoid deprecated methods
     */
    private String formatPrivateKeyAsWIF(byte[] privateKeyBytes, boolean compressed) {
        // Use ECKey helper which handles WIF encoding
        ECKey tempKey = ECKey.fromPrivate(privateKeyBytes, compressed);
        return tempKey.getPrivateKeyAsWiF(params.network());
    }

    /**
     * Validate mnemonic phrase
     */
    public JsonObject validateMnemonic(String mnemonic) {
        JsonObject response = new JsonObject();

        try {
            List<String> words = List.of(mnemonic.trim().split("\\s+"));

            // Check word count
            if (words.size() != 12 && words.size() != 24) {
                response.put("valid", false);
                response.put("error", "Mnemonic must be 12 or 24 words");
                return response;
            }

            // Try to create seed (will throw if invalid)
            DeterministicSeed.ofMnemonic(words, "", Instant.now());

            response.put("valid", true);
            response.put("wordCount", words.size());

        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}
