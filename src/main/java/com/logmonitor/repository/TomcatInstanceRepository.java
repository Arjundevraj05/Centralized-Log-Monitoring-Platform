package com.logmonitor.repository;

import com.logmonitor.entity.TomcatInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TomcatInstanceRepository extends JpaRepository<TomcatInstance, Long> {

    List<TomcatInstance> findByServerIdOrderByInstanceNameAsc(Long serverId);

    Optional<TomcatInstance> findByIdAndServerId(Long id, Long serverId);

    Optional<TomcatInstance> findByServerIdAndCatalinaHome(Long serverId, String catalinaHome);
}
