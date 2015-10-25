package com.github.rmannibucau.cdi.scopes.internal.method;

@FunctionalInterface
public interface SupplierWithException {
    Object run() throws Exception;
}
