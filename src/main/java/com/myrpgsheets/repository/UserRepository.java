package com.myrpgsheets.repository;

import com.myrpgsheets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUserName(String email, String userName);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);
}