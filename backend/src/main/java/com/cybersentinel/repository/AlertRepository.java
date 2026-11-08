package com.cybersentinel.repository;

import com.cybersentinel.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {

    List<Alert> findByStatusOrderByCreatedAtDesc(String status);

    List<Alert> findAllByOrderByCreatedAtDesc();
}
