package com.logmonitor.repository;

import com.logmonitor.entity.TomcatApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TomcatApplicationRepository extends JpaRepository<TomcatApplication, Long> {

    List<TomcatApplication> findByTomcatInstanceIdOrderByAppNameAsc(Long tomcatInstanceId);

    Optional<TomcatApplication> findByIdAndTomcatInstanceId(Long id, Long tomcatInstanceId);

    Optional<TomcatApplication> findByTomcatInstanceIdAndAppName(Long tomcatInstanceId, String appName);
}
