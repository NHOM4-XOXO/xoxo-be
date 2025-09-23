package com.nhom4.xoxo.service.impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.config.CookieConfig;
import com.nhom4.xoxo.dto.req.ForgotPasswordRequest;
import com.nhom4.xoxo.dto.req.LoginRequest;
import com.nhom4.xoxo.dto.req.MailMessage;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.ResetPasswordRequest;
import com.nhom4.xoxo.dto.res.LoginResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.kafka.MailProducer;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.graph.service.SocialGraphService;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import com.nhom4.xoxo.security.JwtTokenProvider;
import com.nhom4.xoxo.service.EmailService;
import com.nhom4.xoxo.service.RefreshTokenService;
import com.nhom4.xoxo.service.TokenService;
import com.nhom4.xoxo.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailProducer mailProducer;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final SocialGraphService socialGraphService;
    private final CookieConfig cookieConfig;

    @Value("${fe.user.base-url}")
    String userBaseUrl;
    @Value("${fe.admin.base-url}")
    String adminBaseUrl;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, MailProducer mailProducer,
            VerificationTokenRepository verificationTokenRepository, RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService, ModelMapper modelMapper, EmailService emailService,
            TokenService tokenService, SocialGraphService socialGraphService, CookieConfig cookieConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailProducer = mailProducer;
        this.verificationTokenRepository = verificationTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.socialGraphService = socialGraphService;
        this.cookieConfig = cookieConfig;
    }

    public static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.replaceAll("[^a-zA-Z0-9]", "");
        return slug.toLowerCase();
    }

    public String generateUsername(String firstName, String lastName) {
        String username = firstName.toLowerCase() + lastName.toLowerCase();
        if (userRepository.existsByUsername(username)) {
            username = username + UUID.randomUUID().toString().substring(0, 4);
        }
        return username;
    }

    @Override
    @Transactional("transactionManager")
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ServiceException("Email already exists");
        }

        String password = request.getPassword();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // Set roles
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);

        String username = generateUsername(toSlug(request.getFirstName()), toSlug(request.getLastName()));
        while (userRepository.existsByUsername(username)) {
            username = generateUsername(toSlug(request.getFirstName()), toSlug(request.getLastName()));
        }
        user.setUsername(username);

        user.setAuthProvider(AuthProvider.LOCAL);
        // LOCAL users cần xác thực email, OAuth2 users được enable mặc định
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        UserResponse userResponse = modelMapper.map(savedUser, UserResponse.class);

        // Sinh token xác thực
        String token = UUID.randomUUID().toString();
        String type = "REGISTER";
        VerificationToken verificationToken = new VerificationToken(token, savedUser,
                LocalDateTime.now().plusHours(24), type);
        verificationTokenRepository.save(verificationToken);

        // Gửi email xác thực trực tiếp (async qua Kafka như trước đây)
        String verifyLink = userBaseUrl + "/verify?token=" + token;
        String htmlContent = String.format(
                """
                        <html>
                            <body>
                                <h2>Xác nhận đăng ký tài khoản</h2>
                                <p>Cảm ơn bạn đã đăng ký tài khoản tại XOXO Social Media.</p>
                                <p>Vui lòng xác nhận email bằng cách bấm vào link sau:</p>
                                <p><a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Xác nhận tài khoản</a></p>
                                <p>Hoặc copy link này: <a href=\"%s\">%s</a></p>
                                <p><b>Token:</b> <span style=\"color: #d32f2f;\">%s</span></p>
                                <p>Link có hiệu lực trong 24 giờ.</p>
                                <p>Trân trọng,<br>Team XOXO</p>
                            </body>
                        </html>
                        """,
                verifyLink, verifyLink, verifyLink, token);

        MailMessage mailMessage = new MailMessage(
                savedUser.getEmail(),
                "Xác nhận đăng ký tài khoản",
                htmlContent);
        try {
            mailProducer.sendMail(mailMessage);
            log.info("Email xác thực đã được gửi thành công qua Kafka cho user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Lỗi gửi email qua Kafka cho user {}: {}", savedUser.getEmail(), e.getMessage(), e);

            // Fallback: Gửi email trực tiếp qua SMTP
            try {
                log.info("Thử gửi email trực tiếp qua SMTP cho user: {}", savedUser.getEmail());
                emailService.sendMail(mailMessage);
                log.info("Email xác thực đã được gửi thành công qua SMTP cho user: {}", savedUser.getEmail());
            } catch (Exception smtpException) {
                log.error("Lỗi gửi email qua SMTP cho user {}: {}", savedUser.getEmail(), smtpException.getMessage(),
                        smtpException);
                // Không throw exception để user vẫn được đăng ký thành công
                // Email có thể được gửi lại sau hoặc admin có thể xử lý thủ công
            }
        }
        return userResponse;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User findById(Long id, User currentUser) {
        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser == null) {
            throw new ServiceException("User not found");
        }
        if (currentUser.getRoles().contains(Role.OWNER)) {
            if (targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Can not view this user");
            }
            return targetUser;
        }
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            if (targetUser.getId().equals(currentUser.getId())) {
                return targetUser;
            }
            if (targetUser.getRoles().contains(Role.ADMIN) || targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Admin can only view user with USER role");
            }
            return targetUser;
        }
        if (currentUser.getRoles().contains(Role.USER)) {
            if (targetUser.getId().equals(currentUser.getId())) {
                return targetUser;
            }
            throw new ForbiddenException("You do not have permission to view this user");
        }

        throw new ForbiddenException("You do not have permission to view this user");
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User updateUser(User user, User currentUser) {
        User targetUser = findById(user.getId(), currentUser);
        if (targetUser == null) {
            throw new ServiceException("User not found");
        }
        if (currentUser.getRoles().contains(Role.OWNER)) {
            if (targetUser.getRoles().contains(Role.OWNER)) {
                throw new ServiceException("Owner cannot update another owner");
            }
            return userRepository.save(user);
        }
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            if (targetUser.getId().equals(currentUser.getId())) {
                return userRepository.save(user);
            }
            if (targetUser.getRoles().contains(Role.ADMIN) || targetUser.getRoles().contains(Role.OWNER)) {
                throw new ServiceException("Admin can only update user with USER role");
            }

            return userRepository.save(user);
        }
        if (currentUser.getRoles().contains(Role.USER)) {
            // Allow keeping the same username; only check when changed
            String newUsername = user.getUsername();
            String oldUsername = targetUser.getUsername();
            if (newUsername != null && !newUsername.equals(oldUsername)) {
                if (userRepository.existsByUsernameAndIdNot(newUsername, targetUser.getId())) {
                    throw new ServiceException("Username already exists");
                }
            }
            if (targetUser.getId().equals(currentUser.getId())) {
                return userRepository.save(user);
            }
            throw new ForbiddenException("You do not have permission to view this user");
        }
        throw new ForbiddenException("You do not have permission to update this user");
    }

    @Override
    public User toggleUserStatus(Long userId, boolean enabled, User currentUser) {
        User targetUser = findById(userId, currentUser);
        if (targetUser == null) {
            throw new ServiceException("User not found");
        }
        if (currentUser.getRoles().contains(Role.OWNER)) {
            if (targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Owner cannot update status of another owner");
            }
            targetUser.setEnabled(enabled);
            targetUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(targetUser);
        }
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            if (targetUser.getRoles().contains(Role.ADMIN) || targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Admin can only update status of user with USER role");
            }
            targetUser.setEnabled(enabled);
            targetUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(targetUser);
        }
        throw new ForbiddenException("You do not have permission to update status of this user");
    }

    @Override
    public void deleteUser(Long userId, User currentUser) {
        User targetUser = findById(userId, currentUser);
        if (targetUser == null) {
            throw new ServiceException("User not found");
        }
        if (currentUser.getRoles().contains(Role.OWNER)) {
            // Owner không được xóa owner khác (kể cả chính mình)
            if (targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Owner cannot delete another owner");
            }
            socialGraphService.deleteUserNode(userId);
            userRepository.deleteById(userId);
            return;
        }
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            // Admin chỉ được xóa user thường
            if (targetUser.getRoles().contains(Role.ADMIN) || targetUser.getRoles().contains(Role.OWNER)) {
                throw new ForbiddenException("Admin can only delete user with USER role");
            }
            socialGraphService.deleteUserNode(userId);
            userRepository.deleteById(userId);
            return;
        }
        throw new ForbiddenException("You do not have permission to delete this user");
    }

    @Override
    public List<UserResponseProjection> findAllUsersAdmin() {
        return userRepository.findAllUserResponsesAdmin();
    }

    @Override
    public List<UserResponseProjection> findAllUsersOwner() {
        return userRepository.findAllUserResponsesOwner();
    }

    @Override
    public User addRoleToUser(Long userId, Role role, User currentUser) {
        User user = findById(userId, currentUser);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Set<Role> roles = user.getRoles();
        if (roles == null) {
            roles = new HashSet<>();
        }

        // if(role.equals(Role.OWNER)){
        // throw new ServiceException("Cannot add OWNER role");
        // }
        roles.add(role);
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User removeRoleFromUser(Long userId, Role role, User currentUser) {
        User user = findById(userId, currentUser); // No currentUser for this operation
        if (user == null) {
            throw new NotFoundException("User not found");
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
    public User setUserRoles(Long userId, Set<Role> roles, User currentUser) {
        User user = findById(userId, currentUser); // No currentUser for this operation
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public User createAdminUser(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new ServiceException("Email already exists");
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
        user.setUsername("admin"+UUID.randomUUID().toString().substring(0, 5));
        while (userRepository.existsByUsername(user.getUsername())) {
            user.setUsername("admin"+UUID.randomUUID().toString().substring(0, 5));
        }

        return userRepository.save(user);
    }


    @Override
    @Transactional(transactionManager = "transactionManager")
    public void forgotPassword(ForgotPasswordRequest request) {
        // Sử dụng TokenService để tạo token (tự động xóa token cũ)
        VerificationToken verificationToken = tokenService.createForgotPasswordToken(request.getEmail());
        User user = verificationToken.getUser();

        Boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        String baseUrl = isAdmin ? adminBaseUrl : userBaseUrl;

        String resetLink = baseUrl + "/reset-password?token=" + verificationToken.getToken();
        String htmlContent = """
                <html>
                    <body>
                        <h2>Reset mật khẩu</h2>
                        <p>Cảm ơn bạn đã yêu cầu reset mật khẩu tại XOXO Social Media.</p>
                        <p>Vui lòng nhấp vào link sau để reset mật khẩu:</p>
                        <p><a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Reset mật khẩu</a></p>
                        <p>Hoặc copy link này: <a href=\"%s\">%s</a></p>
                        <p><b>Token:</b> <span style=\"color: #d32f2f;\">%s</span></p>
                        <p>Link có hiệu lực trong 1 giờ.</p>
                        <p>Trân trọng,<br>Team XOXO</p>
                    </body>
                </html>
                                """
                .formatted(resetLink, resetLink, resetLink, verificationToken.getToken());

        MailMessage mailMessage = new MailMessage(
                user.getEmail(),
                "Reset mật khẩu",
                htmlContent);

        // Sử dụng EmailService để gửi mail với rollback
        emailService.sendMailWithRollback(mailMessage, verificationToken);
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void regenerateForgotPassword(ForgotPasswordRequest request) {
        // Gọi lại method forgotPassword vì logic giống hệt nhau
        forgotPassword(request);
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException("Email không tồn tại trong hệ thống"));

        if (user.isEnabled()) {
            throw new ServiceException("Tài khoản đã được xác thực");
        }

        // Xóa token cũ nếu có
        List<VerificationToken> oldTokens = verificationTokenRepository.findByUserAndType(user, "REGISTER");
        verificationTokenRepository.deleteAll(oldTokens);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user,
                LocalDateTime.now().plusHours(24), "REGISTER");
        verificationTokenRepository.save(verificationToken);

        // Gửi email xác thực
        String verifyLink = userBaseUrl + "/verify?token=" + token;
        String htmlContent = String.format(
                """
                        <html>
                            <body>
                                <h2>Xác nhận đăng ký tài khoản</h2>
                                <p>Cảm ơn bạn đã đăng ký tài khoản tại XOXO Social Media.</p>
                                <p>Vui lòng xác nhận email bằng cách bấm vào link sau:</p>
                                <p><a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Xác nhận tài khoản</a></p>
                                <p>Hoặc copy link này: <a href=\"%s\">%s</a></p>
                                <p><b>Token:</b> <span style=\"color: #d32f2f;\">%s</span></p>
                                <p>Link có hiệu lực trong 24 giờ.</p>
                                <p>Trân trọng,<br>Team XOXO</p>
                            </body>
                        </html>
                        """,
                verifyLink, verifyLink, verifyLink, token);

        MailMessage mailMessage = new MailMessage(
                user.getEmail(),
                "Xác nhận đăng ký tài khoản (Gửi lại)",
                htmlContent);

        try {
            mailProducer.sendMail(mailMessage);
            log.info("Email xác thực đã được gửi lại thành công qua Kafka cho user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Lỗi gửi email qua Kafka cho user {}: {}", user.getEmail(), e.getMessage(), e);

            // Fallback: Gửi email trực tiếp qua SMTP
            try {
                log.info("Thử gửi email trực tiếp qua SMTP cho user: {}", user.getEmail());
                emailService.sendMail(mailMessage);
                log.info("Email xác thực đã được gửi lại thành công qua SMTP cho user: {}", user.getEmail());
            } catch (Exception smtpException) {
                log.error("Lỗi gửi email qua SMTP cho user {}: {}", user.getEmail(), smtpException.getMessage(),
                        smtpException);
                throw new ServiceException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
            }
        }
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void resetPassword(ResetPasswordRequest request) {
        // Sử dụng TokenService để validate token
        VerificationToken verificationToken = tokenService.validateToken(request.getToken(), "FORGOT_PASSWORD");

        // Validate password byte length for BCrypt compatibility
        String newPassword = request.getNewPassword();
        if (newPassword.getBytes().length > 72) {
            throw new ServiceException("Mật khẩu quá dài. Mật khẩu không được vượt quá 72 ký tự.");
        }

        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Xóa token sau khi sử dụng
        tokenService.deleteToken(verificationToken);
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        // 1. Xác thực user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Sinh accessToken (JWT)
        String accessToken = jwtTokenProvider.generateToken(authentication);

        // 3. Lấy user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 4. Sinh refreshToken
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenService.saveRefreshToken(refreshToken, user.getEmail(), 7, TimeUnit.DAYS);

        // 5. Set cookie với cấu hình tối ưu
        ResponseCookie cookie = cookieConfig.createRefreshTokenCookie(refreshToken);
        response.addHeader("Set-Cookie", cookie.toString());

        // 6. Trả về response
        return new LoginResponse(accessToken, user.getEmail(), user.getRoles().toString());
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public String verifyAccount(String token) {
        // Sử dụng TokenService để validate token
        VerificationToken verificationToken = tokenService.validateToken(token, "REGISTER");

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Xóa token sau khi sử dụng
        tokenService.deleteToken(verificationToken);

        return "Xác thực tài khoản thành công. Bạn có thể đăng nhập!";
    }

    @Override
    public String refreshToken(String refreshToken) {
        String userEmail = refreshTokenService.getUserEmailFromRefreshToken(refreshToken);
        if (userEmail == null) {
            throw new ServiceException("Refresh token không hợp lệ.");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        return accessToken;
    }

    @Override
    public boolean logout(String refreshToken, HttpServletResponse response) {
        refreshTokenService.deleteRefreshToken(refreshToken);
        // Clear cookie
        ResponseCookie cookie = cookieConfig.createClearRefreshTokenCookie();
        response.addHeader("Set-Cookie", cookie.toString());
        return true;
    }

    @Override
    public boolean changePassword(String oldPassword, String newPassword, UserDetails currentUser) {

        User user = userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Validate new password byte length for BCrypt compatibility
        if (newPassword.getBytes().length > 72) {
            throw new ServiceException("Mật khẩu mới quá dài. Mật khẩu không được vượt quá 72 ký tự.");
        }

        // Nếu passwordSet = false thì không cần oldPassword
        if (!user.isPasswordSet()) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordSet(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        // Nếu passwordSet = true thì cần oldPassword
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public boolean isAdminOrOwner(User user) {
        return user.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER));
    }

    @Override
    public boolean isSelf(User currentUser, Long userId) {
        return currentUser.getId().equals(userId);
    }

    @Override
    public boolean canViewUser(User currentUser, Long targetUserId) {
        return isSelf(currentUser, targetUserId) || isAdminOrOwner(currentUser);
    }

    @Override
    public boolean canDeleteUser(User currentUser, Long targetUserId) {
        // Không được xóa chính mình, chỉ admin/owner mới được xóa
        return isAdminOrOwner(currentUser) && !isSelf(currentUser, targetUserId);
    }

    @Override
    public boolean canToggleUserStatus(User currentUser) {
        return isAdminOrOwner(currentUser);
    }

    @Override
    public boolean isOwner(User user) {
        return user.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER));
    }

    @Override
    public boolean canAddRole(User currentUser) {
        return isOwner(currentUser);
    }

    @Override
    public boolean canRemoveRole(User currentUser, Role role) {
        return isOwner(currentUser) && !role.equals(Role.OWNER);
    }

    @Override
    public boolean canSetUserRoles(User currentUser) {
        return isOwner(currentUser);
    }

    @Override
    public boolean canCreateAdminUser(User currentUser) {
        return isOwner(currentUser);
    }

    @Override
    public boolean canUpdateUsername(User currentUser) {
        // Deprecated for general use in favor of explicit check with id exclusion
        return true;
    }

    @Override
    public boolean updateUsername(User currentUser, String username) {
        if (username != null && !username.equals(currentUser.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(username, currentUser.getId())) {
                throw new ServiceException("Username already exists");
            }
        }
        currentUser.setUsername(username);
        userRepository.save(currentUser);
        return true;
    }

    @Override
    public boolean updateAvatar(User currentUser, String avatar) {

        currentUser.setAvatarUrl(avatar);
        userRepository.save(currentUser);
        return true;
    }

    @Override
    public boolean updateCover(User currentUser, String cover) {
        currentUser.setCoverUrl(cover);
        userRepository.save(currentUser);
        return true;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        // Nếu không tìm thấy người dùng, ném ngoại lệ
        if (userOptional.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        User user = userOptional.get();

        // Nếu người dùng không được kích hoạt, ném ngoại lệ (tùy thuộc vào logic của
        // bạn)
        // Hoặc bạn có thể thêm logic kiểm tra role ở đây
        if (!user.isEnabled()) {
            throw new NotFoundException("User not found");
        }

        // Nếu người dùng là ADMIN hoặc OWNER, ném ngoại lệ
        // Đây là nơi bạn muốn ngăn việc tìm thấy người dùng có role này
        if (user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER)) {
            throw new NotFoundException("User not found");
        }

        // Nếu tất cả các điều kiện trên không đúng, trả về đối tượng Optional<User>
        return userOptional;
    }

}