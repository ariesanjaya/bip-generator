package com.bipgen.model;

public record MasterKeyResult(String mnemonic, String masterPrivateKey, String masterPublicKey, String seed, boolean hasPassphrase) {
}
