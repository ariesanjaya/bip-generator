package com.bipgen.model;

public record MnemonicValidationResult(boolean valid, int wordCount, String error) {
}
