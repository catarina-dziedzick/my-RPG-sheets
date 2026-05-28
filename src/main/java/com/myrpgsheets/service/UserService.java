package com.myrpgsheets.service;

import com.myrpgsheets.model.User;
import com.myrpgsheets.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        String normalizedEmail = user.getEmail().trim();
        String normalizedUserName = user.getUserName().trim();

        user.setEmail(normalizedEmail);
        user.setUserName(normalizedUserName);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already registered.");
        }

        if (userRepository.existsByUserName(normalizedUserName)) {
            throw new RuntimeException("Username already registered.");
        }

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String identifier, String password) {
        String normalizedIdentifier = identifier.trim();
        Optional<User> userOptional = userRepository.findByEmailOrUserName(normalizedIdentifier, normalizedIdentifier);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}