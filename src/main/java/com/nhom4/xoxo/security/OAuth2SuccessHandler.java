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
import org.springframework.beans.factory.annotation.Value;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${fe.user.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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

        try {
            log.info("[OAuth2SuccessHandler] OAuth2 authentication started");
            
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            // Lấy thông tin từ Google OAuth2
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String givenName = (String) attributes.get("given_name");
            String familyName = (String) attributes.get("family_name");
            String picture = (String) attributes.get("picture");

            log.info("[OAuth2SuccessHandler] OAuth2 user info - Email: {}, Name: {}, Given: {}, Family: {}", 
                    email, name, givenName, familyName);

            if (email == null || email.isEmpty()) {
                log.error("[OAuth2SuccessHandler] Email is null or empty from OAuth2 attributes");
                redirectToError(response, "Email không được cung cấp từ Google");
                return;
            }

            // Kiểm tra user đã tồn tại chưa
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user = null;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                log.info("[OAuth2SuccessHandler] Existing user found: {}", user.getEmail());
                
                // Nếu user bị disabled nhưng đăng nhập OAuth2, tự động enable
                if (!user.isEnabled()) {
                    log.info("[OAuth2SuccessHandler] Enabling disabled user for OAuth2 login: {}", user.getEmail());
                    user.setEnabled(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    log.info("[OAuth2SuccessHandler] User enabled successfully: {}", user.getEmail());
                }
                
                // Cập nhật thông tin mới từ Google nếu cần
                if (picture != null && !picture.equals(user.getAvatarUrl())) {
                    user.setAvatarUrl(picture);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    log.info("[OAuth2SuccessHandler] Updated avatar for existing user: {}", user.getEmail());
                }
                
            } else {
                log.info("[OAuth2SuccessHandler] Creating new user for email: {}", email);
                
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
                String randomPassword = UUID.randomUUID().toString();
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
                log.info("[OAuth2SuccessHandler] New user created successfully: {} (ID: {})", 
                        user.getEmail(), user.getId());
            }

            // Tạo JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            log.info("[OAuth2SuccessHandler] JWT token generated successfully for user: {}", email);

            String refreshToken = UUID.randomUUID().toString();
            refreshTokenService.saveRefreshToken(refreshToken, user.getEmail(), 7, TimeUnit.DAYS);
            
            // Set refreshToken vào HttpOnly cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // true nếu dùng HTTPS
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict")
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            // Redirect về frontend với token
            String redirectUrl = frontendBaseUrl + "/oauth2/success?token=" + jwt;
            log.info("[OAuth2SuccessHandler] Redirecting to: {}", redirectUrl);
            
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            log.error("[OAuth2SuccessHandler] Error during OAuth2 authentication: {}", e.getMessage(), e);
            redirectToError(response, "Lỗi xác thực OAuth2: " + e.getMessage());
        }
    }
    
    private void redirectToError(HttpServletResponse response, String errorMessage) throws IOException {
        String redirectUrl = frontendBaseUrl + "/oauth2/error?message=" + 
                java.net.URLEncoder.encode(errorMessage, "UTF-8");
        log.error("[OAuth2SuccessHandler] Redirecting to error page: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}