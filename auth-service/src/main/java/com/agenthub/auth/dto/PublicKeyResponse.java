package com.agenthub.auth.dto;

public class PublicKeyResponse {

    private String publicKey;
    private String algorithm;

    public PublicKeyResponse() {
    }

    public PublicKeyResponse(String publicKey, String algorithm) {
        this.publicKey = publicKey;
        this.algorithm = algorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public PublicKeyResponse setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public PublicKeyResponse setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }
}
