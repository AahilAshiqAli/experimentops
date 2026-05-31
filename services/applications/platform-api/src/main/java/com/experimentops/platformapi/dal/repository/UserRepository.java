package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<User> findByEmailAndWorkspaceUuidAndStatusAndEnabled(String email, String workspaceUuid, StatusEnum status, boolean enabled);

}
