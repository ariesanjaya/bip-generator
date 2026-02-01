package com.bipgen.model;

public record BIP38DecryptResult(String privateKey, String address, boolean compressed) {
}
