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
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
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

        };
    }
}
