package com.logmonitor.repository;

import com.logmonitor.entity.ApplicationLogConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationLogConfigRepository extends JpaRepository<ApplicationLogConfig, Long> {

    Optional<ApplicationLogConfig> findByTomcatApplicationId(Long tomcatApplicationId);

    Optional<ApplicationLogConfig> findByIdAndTomcatApplication_TomcatInstance_Server_Id(
            Long id, Long serverId);
}
