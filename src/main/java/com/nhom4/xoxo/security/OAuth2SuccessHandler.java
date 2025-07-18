package com.nhom4.xoxo.security;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
        
        System.out.println("🔍 OAuth2 Attributes:");
        System.out.println("  - Email: " + email);
        System.out.println("  - Name: " + name);
        System.out.println("  - Given Name: " + givenName);
        System.out.println("  - Family Name: " + familyName);
        System.out.println("  - Picture: " + picture);
        
        // Kiểm tra user đã tồn tại chưa
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("✅ User already exists: " + user.getEmail());
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
            
            // Set password mặc định cho OAuth2 users
            user.setPassword("OAUTH2_USER_PASSWORD");
            user.setEnabled(true);
            user.setAuthProvider(AuthProvider.GOOGLE);
            
            // Set roles
            Set<Role> roles = new HashSet<>();
            roles.add(Role.USER);
            user.setRoles(roles);
            
            // Set timestamps
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            user = userRepository.save(user);
            System.out.println("✅ New user created: " + user.getEmail());
            System.out.println("  - First Name: " + user.getFirstName());
            System.out.println("  - Last Name: " + user.getLastName());
        }
        
        try {
            // Tạo JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            
            System.out.println("✅ OAuth2 login successful for user: " + email);
            System.out.println("🔑 JWT token generated: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
            
            // Redirect về frontend với token
            String redirectUrl = "http://localhost:3000/oauth2/success?token=" + jwt;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            System.err.println("❌ Error in OAuth2 success handler: " + e.getMessage());
            e.printStackTrace();
            // Fallback: redirect về frontend với thông báo lỗi
            String redirectUrl = "http://localhost:3000/oauth2/error?message=Token generation failed: " + e.getMessage();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
} 