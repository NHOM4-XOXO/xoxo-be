package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.RegisterRequest;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.AuthProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nhom4.xoxo.dto.MailMessage;
import com.nhom4.xoxo.service.MailProducer;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.nhom4.xoxo.entity.Role;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailProducer mailProducer;
    private final VerificationTokenRepository verificationTokenRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, MailProducer mailProducer, VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailProducer = mailProducer;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Set roles
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);
        
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);

        // Sinh token xác thực
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, savedUser, LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(verificationToken);

        // Gửi email xác thực
        String verifyLink = "http://localhost:8080/api/auth/verify?token=" + token;
        String htmlContent = """
            <html>
            <body>
                <h2>Xác nhận đăng ký tài khoản</h2>
                <p>Cảm ơn bạn đã đăng ký tài khoản tại XOXO Social Media.</p>
                <p>Vui lòng xác nhận email bằng cách bấm vào link sau:</p>
                <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Xác nhận tài khoản</a></p>
                <p>Hoặc copy link này: <a href="%s">%s</a></p>
                <p>Link có hiệu lực trong 24 giờ.</p>
                <p>Trân trọng,<br>Team XOXO</p>
            </body>
            </html>
            """.formatted(verifyLink, verifyLink, verifyLink);
        
        MailMessage mailMessage = new MailMessage(
            savedUser.getEmail(),
            "Xác nhận đăng ký tài khoản",
            htmlContent
        );
        mailProducer.sendMail(mailMessage);
        return savedUser;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User toggleUserStatus(Long userId, boolean enabled) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User addRoleToUser(Long userId, Role role) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        Set<Role> roles = user.getRoles();
        if (roles == null) {
            roles = new HashSet<>();
        }
        
        roles.add(role);
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    @Override
    public User removeRoleFromUser(Long userId, Role role) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        Set<Role> roles = user.getRoles();
        if (roles != null) {
            roles.remove(role);
            user.setRoles(roles);
            user.setUpdatedAt(LocalDateTime.now());
        }
        
        return userRepository.save(user);
    }

    @Override
    public User setUserRoles(Long userId, Set<Role> roles) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    @Override
    public User createAdminUser(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        // Set roles
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ADMIN);
        user.setRoles(roles);
        
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
} 