package DatabaseController;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.io.FileInputStream;

public class CryptoUtil {

    private static final String ALGO = "AES";


    public static String encrypt(String data) throws Exception {
        SecretKey key = fetchKey();
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    public static String decrypt(String encrypted) throws Exception {
        SecretKey key = fetchKey();
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    }

    private static SecretKey fetchKey() throws Exception{
        Properties props = new Properties();
        props.load(new FileInputStream("db.properties"));
        String keyStr = props.getProperty("db.encryptionKey");
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}