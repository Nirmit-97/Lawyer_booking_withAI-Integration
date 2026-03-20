package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndPassword(String username, String password);
    boolean existsByUsername(String username);
    Optional<User> findFirstByEmail(String email);
    boolean existsByEmail(String email);
}

