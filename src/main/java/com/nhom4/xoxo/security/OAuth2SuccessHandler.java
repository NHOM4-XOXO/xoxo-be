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
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        
        // Kiểm tra user đã tồn tại chưa
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isEmpty()) {
            // Tạo user mới nếu chưa tồn tại
            User newUser = new User();
            newUser.setEmail(email);
            // Không set password cho OAuth2 users
            newUser.setEnabled(true);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            
            // Set roles
            Set<Role> roles = new HashSet<>();
            roles.add(Role.USER);
            newUser.setRoles(roles);
            
            // Set timestamps
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(newUser);
        }
        
        try {
            // Tạo JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);
            
            // Redirect về frontend với token
            String redirectUrl = "http://localhost:3000/oauth2/success?token=" + jwt;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            // Fallback: redirect về frontend với thông báo lỗi
            String redirectUrl = "http://localhost:3000/oauth2/error?message=Token generation failed";
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
} 