package com.nhom4.xoxo.security;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.replaceAll("[^a-zA-Z0-9]", ""); 
        return slug.toLowerCase();
    }
    public String generateUsername(String firstName, String lastName) {
        String username = toSlug(firstName) + toSlug(lastName);
        if (userRepository.existsByUsername(username)) {
            username = username + UUID.randomUUID().toString().substring(0, 4);
        }
        return username;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Lấy thông tin từ Google OAuth2
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");

        // Kiểm tra user đã tồn tại chưa
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user = null;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.isEnabled()) {
                try {
                    String redirectUrl = "https://google.com";
                    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            // Tạo user mới
            user = new User();
            user.setEmail(email);

            // Parse name thành firstName và lastName
            String firstName = givenName;
            String lastName = familyName;

            // Nếu không có given_name/family_name, parse từ name
            if (firstName == null || firstName.isEmpty()) {
                if (name != null && !name.isEmpty()) {
                    String[] nameParts = name.split(" ", 2);
                    firstName = nameParts[0];
                    lastName = nameParts.length > 1 ? nameParts[1] : "";
                } else {
                    firstName = "User";
                    lastName = "";
                }
            }

            user.setFirstName(firstName);
            user.setLastName(lastName);

            // Set password mặc định cho OAuth2 users (random, đã mã hóa)
            String randomPassword = java.util.UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setEnabled(true);
            user.setAuthProvider(AuthProvider.GOOGLE);
            String username = generateUsername(user.getFirstName(), user.getLastName());
            while (userRepository.existsByUsername(username)) {
                username = generateUsername(user.getFirstName(), user.getLastName());
            }
            user.setUsername(username);
            user.setAvatarUrl(picture);
            user.setCoverUrl(picture);

            // Set roles
            Set<Role> roles = new HashSet<>();
            roles.add(Role.USER);
            user.setRoles(roles);

            // Set timestamps
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            user = userRepository.save(user);

        }

        try {
            // Tạo JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            System.out.println("✅ OAuth2 login successful for user: " + email);
            System.out.println("🔑 JWT token generated: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");

            String refreshToken = java.util.UUID.randomUUID().toString();

            refreshTokenService.saveRefreshToken(refreshToken, user.getEmail(), 7, TimeUnit.DAYS);
            // Set refreshToken vào HttpOnly cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict")
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            String redirectUrl;

            // Mặc định redirect về FE
            redirectUrl = "http://localhost:3000/oauth2/success?token=" + jwt;

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
            String redirectUrl = "http://localhost:3000/oauth2/error?message=Token generation failed: "
                    + e.getMessage();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}