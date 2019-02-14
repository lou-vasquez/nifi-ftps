package kullervo16.audit.utils;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;

public class CryptoUtils {

    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";
    public static final Properties properties = new Properties();
    public static final String transform = "AES/CBC/PKCS5Padding";
    private static Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public static KeyPair generateKeyPair() {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }







    public static PrivateKey readPrivateKey(File f)
            throws Exception {


        byte[] content = readContent(f);

        System.out.println("Private key = "+ Base64.encodeBase64String(content));

        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(content);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static byte[] readContent(File f) throws IOException {
        byte[] content;
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            IOUtils.copy(fis, bos);
            content = bos.toByteArray();
        }
        return content;
    }

    public static PublicKey readPublicKey(File f)
            throws Exception {



        byte[] content = readContent(f);
        System.out.println(content.length+" Public key = "+Base64.encodeBase64String(content));

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(content);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static String signValue(PrivateKey pk, String input) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, UnsupportedEncodingException, SignatureException {

        Signature privateSignature = Signature.getInstance("SHA512withRSA");

        privateSignature.initSign(pk);

        privateSignature.update(input.getBytes("UTF-8"));

        byte[] s = privateSignature.sign();

        return Base64.encodeBase64String(s);

    }
}
