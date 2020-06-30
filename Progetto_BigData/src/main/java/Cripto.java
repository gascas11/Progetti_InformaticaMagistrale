import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

//Classe che viene utilizzata cripatare e decriptare i dati
public class Cripto {
    private static final String UNICODE_FORMAT = "UTF8";
    private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private Cipher cipher;
    private String myEncryptionKey;
    private SecretKey key;

    //creo il mio oggetto da cui cripterò\decripterò i dati, richiede una chiave
    public Cripto(String encryptionKey) throws Exception {
        myEncryptionKey = encryptionKey;
        String myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
        byte[] arrayBytes = myEncryptionKey.getBytes(UNICODE_FORMAT);
        KeySpec ks = new DESedeKeySpec(arrayBytes);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(myEncryptionScheme);
        cipher = Cipher.getInstance(myEncryptionScheme);
        key = skf.generateSecret(ks);
    }

    //metodo che data una stringa la cripta usando la chiave del cifratore
    public String encrypt(String unencryptedString) {
        String encryptedString = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainText = unencryptedString.getBytes(UNICODE_FORMAT);
            byte[] encryptedText = cipher.doFinal(plainText);
            encryptedString = new String(Base64.encodeBase64(encryptedText));
        } catch (Exception e) {
            System.out.println("ERROR: "+unencryptedString);
            e.printStackTrace();
        }
        return encryptedString;
    }

    //metodo che data una stringa la decripta usando la chiave del cifratore
    public String decrypt(String encryptedString) {
        String decryptedText=null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.decodeBase64(encryptedString);
            byte[] plainText = cipher.doFinal(encryptedText);
            decryptedText= new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }


    public String getEncryptionKey() {
        return myEncryptionKey;
    }

    //metodo statico che effettua l'hashing (sha256) di una stringa (password)
    public static String SHA256(String s) {
        return new String(Hex.encodeHex(DigestUtils.sha256(s)));
    }

}
