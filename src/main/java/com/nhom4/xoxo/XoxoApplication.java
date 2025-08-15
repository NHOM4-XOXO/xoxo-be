package com.nhom4.xoxo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.enums.FriendshipStatus;
import com.nhom4.xoxo.repository.FriendshipRepository;
import com.nhom4.xoxo.graph.service.SocialGraphService;

@SpringBootApplication
public class XoxoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XoxoApplication.class, args);
    }

    @Bean
    public CommandLineRunner createSampleAccounts(UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            SocialGraphService socialGraphService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // OWNER
            String ownerEmail = "owner@xoxo.com";
            if (userRepository.findByEmail(ownerEmail).isEmpty()) {
                User owner = new User();
                owner.setEmail(ownerEmail);
                owner.setPassword(passwordEncoder.encode("Owner123@"));
                owner.setFirstName("Owner");
                owner.setLastName("Account");
                Set<Role> roles = new HashSet<>();
                roles.add(Role.OWNER);
                owner.setRoles(roles);
                owner.setAuthProvider(AuthProvider.LOCAL);
                owner.setEnabled(true);
                owner.setCreatedAt(LocalDateTime.now());
                owner.setUpdatedAt(LocalDateTime.now());
                owner.setUsername("owner");
                userRepository.save(owner);

            }
            // ADMIN
            String adminEmail = "admin@xoxo.com";
            if (userRepository.findByEmail(adminEmail).isEmpty())
             {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("Admin123@"));
                admin.setFirstName("Admin");
                admin.setLastName("Account");
                Set<Role> roles = new HashSet<>();
                roles.add(Role.ADMIN);
                admin.setRoles(roles);
                admin.setAuthProvider(AuthProvider.LOCAL);
                admin.setEnabled(true);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                admin.setUsername("admin");
                userRepository.save(admin);

            }
            // 100 USERS
            int totalUsers = 100;
            for (int i = 1; i <= totalUsers; i++) {
                String email = "user" + i + "@xoxo.com";
                if (userRepository.findByEmail(email).isPresent())
                    continue;
                User u = new User();
                u.setEmail(email);
                u.setPassword(passwordEncoder.encode("User123@"));
                u.setFirstName("User" + i);
                u.setLastName("Seed");
                Set<Role> roles = new HashSet<>();
                roles.add(Role.USER);
                u.setRoles(roles);
                u.setAuthProvider(AuthProvider.LOCAL);
                u.setEnabled(true);
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());
                u.setUsername("user" + i);
                u = userRepository.save(u);
                // ensure node in Neo4j
                socialGraphService.ensureUserNode(u.getId(), u.getUsername());
            }
            // Sau khi đã tạo allUsers:
            var allUsers = userRepository.findAll();

            // Helper tạo kết bạn nếu chưa có và sync Neo4j
            java.util.function.BiConsumer<User, User> link = (a, b) -> {
                if (!a.getId().equals(b.getId())
                        && friendshipRepository.findAnyFriendshipBetweenUsers(a, b).isEmpty()) {
                    Friendship f = Friendship.builder()
                            .user(a).friend(b).initiator(a)
                            .status(FriendshipStatus.ACCEPTED)
                            .build();
                    friendshipRepository.save(f);
                    socialGraphService.connectFriends(a.getId(), a.getUsername(), b.getId(), b.getUsername());
                }
            };

            // 1) Vòng tròn + nối dày K láng giềng kế tiếp (K=3)
            int n = allUsers.size();
            int K = 3;
            for (int i = 0; i < n; i++) {
                User a = allUsers.get(i);
                for (int k = 1; k <= K; k++) {
                    User b = allUsers.get((i + k) % n);
                    link.accept(a, b);
                }
            }

            // 2) Thêm các kết nối chéo xa để tăng bạn chung
            for (int i = 0; i < n; i += 5) {
                User a = allUsers.get(i);
                User b = allUsers.get((i + 10) % n);
                link.accept(a, b);
            }

        };
    }
}
