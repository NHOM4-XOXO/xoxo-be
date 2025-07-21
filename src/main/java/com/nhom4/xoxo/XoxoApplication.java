package com.nhom4.xoxo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@SpringBootApplication
public class XoxoApplication {

	public static void main(String[] args) {
		SpringApplication.run(XoxoApplication.class, args);
	}

	@Bean
	public CommandLineRunner createSampleAccounts(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// OWNER
			String ownerEmail = "owner@xoxo.com";
			if (userRepository.findByEmail(ownerEmail).isEmpty()) {
				User owner = new User();
				owner.setEmail(ownerEmail);
				owner.setPassword(passwordEncoder.encode("owner123"));
				owner.setFirstName("Owner");
				owner.setLastName("Account");
				Set<Role> roles = new HashSet<>();
				roles.add(Role.OWNER);
				owner.setRoles(roles);
				owner.setAuthProvider(AuthProvider.LOCAL);
				owner.setEnabled(true);
				owner.setCreatedAt(LocalDateTime.now());
				owner.setUpdatedAt(LocalDateTime.now());
				userRepository.save(owner);
				System.out.println("[INIT] Created OWNER account: " + ownerEmail);
			}
			// ADMIN
			String adminEmail = "admin@xoxo.com";
			if (userRepository.findByEmail(adminEmail).isEmpty()) {
				User admin = new User();
				admin.setEmail(adminEmail);
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setFirstName("Admin");
				admin.setLastName("Account");
				Set<Role> roles = new HashSet<>();
				roles.add(Role.ADMIN);
				admin.setRoles(roles);
				admin.setAuthProvider(AuthProvider.LOCAL);
				admin.setEnabled(true);
				admin.setCreatedAt(LocalDateTime.now());
				admin.setUpdatedAt(LocalDateTime.now());
				userRepository.save(admin);
				System.out.println("[INIT] Created ADMIN account: " + adminEmail);
			}
			// USER
			String userEmail = "user@xoxo.com";
			if (userRepository.findByEmail(userEmail).isEmpty()) {
				User user = new User();
				user.setEmail(userEmail);
				user.setPassword(passwordEncoder.encode("user123"));
				user.setFirstName("User");
				user.setLastName("Account");
				Set<Role> roles = new HashSet<>();
				roles.add(Role.USER);
				user.setRoles(roles);
				user.setAuthProvider(AuthProvider.LOCAL);
				user.setEnabled(true);
				user.setCreatedAt(LocalDateTime.now());
				user.setUpdatedAt(LocalDateTime.now());
				userRepository.save(user);
				System.out.println("[INIT] Created USER account: " + userEmail);
			}
		};
	}
}
