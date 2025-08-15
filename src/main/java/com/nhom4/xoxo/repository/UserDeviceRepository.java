package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom4.xoxo.entity.UserDevice;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
	Optional<UserDevice> findByDeviceId(String deviceId);
	List<UserDevice> findByUserId(Long userId);
}
