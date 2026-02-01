package com.bipgen.model;

import java.util.List;

public record DerivedMultipleResult(List<DerivedAddressResult> addresses, int count) {
}
