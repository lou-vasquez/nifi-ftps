package org.apache.nifi.processors.standard.util;

import org.apache.nifi.logging.ComponentLog;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathBuilderResult;
import java.util.HashSet;

/**
 * This class handles the verification of the remote side.
 */
public class HostVerifier implements HostnameVerifier {
    private final boolean allowSelfSigned;
    private final ComponentLog logger;

    public HostVerifier(boolean allowSelfSigned, ComponentLog logger) {
        this.allowSelfSigned = allowSelfSigned;
        this.logger = logger;
    }

    @Override
    public boolean verify(String serverName, SSLSession sslSession) {
        try {

            CertificateFactory certFactory = CertificateFactory.getInstance("x.509");
            for(X509Certificate cert : sslSession.getPeerCertificateChain()) {
                // convert to java.cert (sslSession uses deprected javax.cert)
                try(ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded())) {
                    java.security.cert.X509Certificate convertedCert = (java.security.cert.X509Certificate) certFactory.generateCertificate(bis);
                    if(CertificateVerifier.isSelfSigned(convertedCert)) {
                        if(this.allowSelfSigned) {
                            // make sure the hostname corresponds to the CN
                            if(!cert.getIssuerDN().getName().contains("CN="+serverName)) {
                                this.logger.error("Refusing certificate for "+cert.getIssuerDN()+" because the CN does not correspond with the server name "+serverName);
                                return false;
                            }
                        } else {
                            this.logger.error("Refusing certificate for "+cert.getIssuerDN()+" because self-signed and no permission to trust self-signed certificates.");
                            return false;
                        }
                    } else {
                        PKIXCertPathBuilderResult verificationResult = CertificateVerifier.verifyCertificate(convertedCert, new HashSet<>());
                        logger.info("Verified chain "+verificationResult);
                    }
                } catch (IOException|CertificateVerificationException e) {
                    logger.error(e.getMessage(),e);
                    return false;
                }
            }
        } catch (SSLPeerUnverifiedException|CertificateException|NoSuchAlgorithmException|CertificateEncodingException|NoSuchProviderException e) {
            logger.error(e.getMessage(),e);
            return false;
        }
        return true;
    }
}
