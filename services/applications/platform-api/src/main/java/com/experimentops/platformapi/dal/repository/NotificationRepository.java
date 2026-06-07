package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByUuidAndEnabled(String uuid, boolean enabled);
}
