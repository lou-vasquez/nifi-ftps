package kullervo16.audit.utils;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.security.KeyPair;

import static org.junit.Assert.*;

public class CryptoUtilsTest {

    @Test
    public void testCrypto() throws Exception {
        KeyPair keypair = CryptoUtils.generateKeyPair();
        System.out.println(Base64.encodeBase64String(keypair.getPrivate().getEncoded()));
        String toBeSigned1 = "textToBeSigned";
        String toBeSigned2 = "textToBeSigned";
        String toBeSigned3 = "otherTextToBeSigned";
        String signature1 = CryptoUtils.signValue(keypair.getPrivate(), toBeSigned1);
        String signature2 = CryptoUtils.signValue(keypair.getPrivate(), toBeSigned2);
        String signature3 = CryptoUtils.signValue(keypair.getPrivate(), toBeSigned3);
        KeyPair keypair2 = CryptoUtils.generateKeyPair();
        String signature4 = CryptoUtils.signValue(keypair2.getPrivate(), toBeSigned1);

        System.out.println(signature1);
        System.out.println(signature2);
        System.out.println(signature3);
        System.out.println(signature4);

        assertEquals(signature1, signature2);
        assertNotEquals(signature1, signature3);
        assertNotEquals(signature1, signature4);
    }
}
