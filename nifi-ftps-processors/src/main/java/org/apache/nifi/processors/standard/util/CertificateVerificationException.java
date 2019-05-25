package org.apache.nifi.processors.standard.util;

public class CertificateVerificationException extends Exception {
    public CertificateVerificationException(String s, Exception ex) {
        super(s, ex);
    }

    public CertificateVerificationException(String s) {
        super(s);
    }

}
