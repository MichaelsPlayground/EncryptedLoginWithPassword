package de.androidcrypto.encryptedloginwithpassword;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class EncryptionVerifyAppPasswordUtil implements Runnable {
    // solution taken from https://stackoverflow.com/a/9148954/8166854
    private volatile String hashedPasswordBase64;

    private char[] password; // this is the password to verify
    private String storedHashedPasswordBase64;
    private String storedSaltBase64;
    private boolean verificationResult = false;

    public void setPassword(char[] password) {
        this.password = password;
    }
    public void setStoredSaltBase64(String storedSaltBase64) {
        this.storedSaltBase64 = storedSaltBase64;
    }
    public void setStoredHashedPasswordBase64(String storedHashedPasswordBase64) {
        this.storedHashedPasswordBase64 = storedHashedPasswordBase64;
    }
    public boolean getVerificationResult() {
        return verificationResult;
    }

    @Override
    public void run() {
        // do PBKDF2 on the entered password and verify with the stored one
        System.out.println("*** runThread before doPbkdf2");
        byte[][] result = doPbkdf2(password, storedSaltBase64);
        System.out.println("*** runThread after doPbkdf2");
        if (result == null) {
            verificationResult = false;
        } else {
            hashedPasswordBase64 = base64Encoding(result[1]);
            if (hashedPasswordBase64.equals(storedHashedPasswordBase64)) {
                verificationResult = true;
            } else {
                verificationResult = false;
            }
        }
    }

    // this method is running in a thread, so don't update the ui directly
    // return[0] = salt
    // return[1] = key
    private byte[][] doPbkdf2(char[] passphraseChar, String saltBase64) {
        final int PBKDF2_ITERATIONS = 10000; // fixed as minimum
        int saltLength = 64;
        int keyLength = 64;
        // generate 64 byte random salt for pbkdf2
        byte[] salt = base64Decoding(saltBase64);
        if (salt.length != saltLength) {
            return null;
        }
        byte[] secretKey = new byte[0];
        SecretKeyFactory secretKeyFactory = null;
        // we are deriving the secretKey from the passphrase with PBKDF2 and using
        // the hash algorithm Hmac256, this is built in from SDK >= 26
        // for older SDKs we are using the own PBKDF2 function
        // api between 23 - 25
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                // uses 3rd party PBKDF function to get PBKDF2withHmacSHA256
                // PBKDF2withHmacSHA256	is available API 26+
                byte[] passphraseByte = charArrayToByteArray(passphraseChar);
                secretKey = PBKDF.pbkdf2("HmacSHA256", passphraseByte, salt, PBKDF2_ITERATIONS, keyLength);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                Log.e("APP_TAG", "generateAndStoreSecretKeyFromPassphrase error: " + e.toString());
                return null;
            }
        }
        // api 26+ has HmacSHA256 available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                KeySpec keySpec = new PBEKeySpec(passphraseChar, salt, PBKDF2_ITERATIONS, keyLength * 8);
                secretKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
                return null;
            }
        }
        byte[][] returnData = new byte[2][];
        returnData[0] = salt.clone();
        returnData[1] = secretKey.clone();
        return returnData;
    }

    // https://stackoverflow.com/a/9670279/8166854
    byte[] charArrayToByteArray(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(chars, '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    private static String base64Encoding(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    private static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }
}
