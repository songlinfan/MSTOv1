package common;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class Config {
    private static final Properties props = new Properties();
    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("config.properties is not found!");
            }
        } catch (IOException e) {
            throw new RuntimeException("read failure", e);
        }
    }
    public static String get(String key) {
        return props.getProperty(key);
    }
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static void main(String[] args) throws IOException {

    }
}
