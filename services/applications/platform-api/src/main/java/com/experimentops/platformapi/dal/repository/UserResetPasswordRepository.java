package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.entity.UserResetPassword;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserResetPasswordRepository extends JpaRepository<UserResetPassword, Long> {

    Optional<UserResetPassword> findByUuidAndEnabled(String uuid, boolean enabled);
}