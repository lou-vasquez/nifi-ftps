package org.apache.nifi.processors.standard.util;

import org.apache.nifi.logging.ComponentLog;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.CertificateEncodingException;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class handles the verification of the remote side.
 * @author Jef Verelst
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
        String principalName = "unknown";
        try {

            X509Certificate lastCert = ((X509Certificate[])sslSession.getPeerCertificates())[0];
            Set<X509Certificate> chain = new HashSet<>();
            for(X509Certificate cert : (X509Certificate[])sslSession.getPeerCertificates()) {
                chain.add(cert);
            }

            principalName = sslSession.getPeerPrincipal().getName();

            InetAddress addr = InetAddress.getByName(serverName);
            serverName = addr.getHostName(); // handle the case where an IP address comes in... we need to make sure we have the hostname to compare the CN


            if(CertificateVerifier.isSelfSigned(lastCert)) {
                if(this.allowSelfSigned) {
                    // make sure the hostname corresponds to the CN
                    if(!sslSession.getPeerPrincipal().getName().contains("CN="+serverName)) {
                        this.logger.error("Refusing certificate for "+principalName+" because the CN does not correspond with the server name "+serverName);
                        return false;
                    }
                } else {
                    this.logger.error("Refusing certificate for "+principalName+" because self-signed and no permission to trust self-signed certificates.");
                    return false;
                }
            } else {
                PKIXCertPathBuilderResult verificationResult = CertificateVerifier.verifyCertificate(lastCert, chain);
                if(logger.isTraceEnabled()) {
                    logger.trace("Verified chain " + verificationResult);
                }
                if(!principalName.contains("CN="+serverName)) {
                    this.logger.error("Refusing certificate for "+principalName+" because the CN does not correspond with the server name "+serverName);
                    return false;
                }
            }

        } catch(CertificateVerificationException cpbe) {
            logger.error("Refusing certificate for "+principalName+" because the certificate chain could not be validated.");
            return false;
        } catch (UnknownHostException|SSLPeerUnverifiedException|CertificateException|NoSuchAlgorithmException|NoSuchProviderException e) {
            logger.error(e.getMessage(),e);
            return false;
        }
        return true;
    }
}
