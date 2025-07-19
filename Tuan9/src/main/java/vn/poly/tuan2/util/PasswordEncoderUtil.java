package vn.poly.tuan2.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Mật khẩu đã mã hóa cho '" + rawPassword + "': " + encodedPassword);
    }
}
