package common;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class SecretKeyGenerator {
    private static final String SecretKeyPath = Config.get("SecretKeyPath");

    public void generateAndSaveKey() throws Exception {
        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        byte[] encoded = key.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(encoded);

        Files.createDirectories(Paths.get(SecretKeyPath).getParent());
        try (FileOutputStream fos = new FileOutputStream(SecretKeyPath)) {
            fos.write(base64Key.getBytes());
        }
    }

    public static SecretKey loadKey() {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(SecretKeyPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String base64Key = new String(bytes);
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);

        return new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public static void main(String[] args) throws Exception {
        SecretKeyGenerator keyGenerator = new SecretKeyGenerator();
        keyGenerator.generateAndSaveKey();
        System.out.println("Secret key has been generated and saved to file: " + SecretKeyPath);
    }
}
