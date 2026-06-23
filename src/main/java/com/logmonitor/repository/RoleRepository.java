package com.logmonitor.repository;

import com.logmonitor.entity.Role;
import com.logmonitor.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Role} persistence operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a role by its enumerated name.
     *
     * @param roleName the role name to look up
     * @return optional containing the role if found
     */
    Optional<Role> findByRoleName(RoleName roleName);
}
