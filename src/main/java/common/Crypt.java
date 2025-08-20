package common;


import MST.Bucket;

import javax.crypto.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class Crypt{
    private static final String ALGORITHM = "AES";

    private static byte[] encrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private static byte[] decrypt(byte[] encryptedData, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static byte[] encryptBucket(Bucket bucket, SecretKey key) {
        try {
            byte[] serialized = serialize(bucket);
            return encrypt(serialized, key);
        } catch (Exception e) {
            throw new RuntimeException("Bucket encryption failed", e);
        }
    }

    public static Bucket decryptBucket(byte[] encryptedBytes, SecretKey key) {
        try {
            byte[] decrypted = decrypt(encryptedBytes, key);
            return (Bucket) deserialize(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Bucket decryption failed", e);
        }
    }

    public static byte[] serialize(Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }


    public static Object deserialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }


}
