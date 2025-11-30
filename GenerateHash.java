import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("user123: " + encoder.encode("user123"));
    }
}
