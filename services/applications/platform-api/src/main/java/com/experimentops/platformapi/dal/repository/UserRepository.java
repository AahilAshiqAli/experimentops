package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.entity.Workspace;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<Workspace> findByUuidAndStatusAndEnabled(String uuid, StatusEnum status, boolean enabled);

    Optional<Workspace> findByEmailAndStatusAndEnabled(String name, String email, StatusEnum status, boolean enabled);

}
