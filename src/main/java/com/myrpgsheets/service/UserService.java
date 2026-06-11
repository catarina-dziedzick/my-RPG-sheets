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

    public User updateProfile(
            User loggedUser,
            String name,
            String email,
            String userName,
            String displayName,
            String bio,
            String preferredSystem
    ) {
        User user = userRepository.findById(loggedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found."));

        String normalizedName = name != null ? name.trim() : "";
        String normalizedEmail = email != null ? email.trim() : "";
        String normalizedUserName = userName != null ? userName.trim() : "";

        if (normalizedName.isBlank()) {
            throw new RuntimeException("O nome completo é obrigatório.");
        }

        if (normalizedEmail.isBlank()) {
            throw new RuntimeException("O e-mail é obrigatório.");
        }

        if (normalizedUserName.isBlank()) {
            throw new RuntimeException("O nome de usuário é obrigatório.");
        }

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new RuntimeException("Este e-mail já está em uso.");
        }

        if (userRepository.existsByUserNameAndIdNot(normalizedUserName, user.getId())) {
            throw new RuntimeException("Este nome de usuário já está em uso.");
        }

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setUserName(normalizedUserName);
        user.setDisplayName(displayName != null ? displayName.trim() : null);
        user.setBio(bio != null ? bio.trim() : null);
        user.setPreferredSystem(preferredSystem != null ? preferredSystem.trim() : null);

        return userRepository.save(user);
    }

    public User changePassword(
            User loggedUser,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {
        User user = userRepository.findById(loggedUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (currentPassword == null || !user.getPassword().equals(currentPassword)) {
            throw new RuntimeException("Senha atual incorreta.");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("A nova senha é obrigatória.");
        }

        if (newPassword.length() < 4) {
            throw new RuntimeException("A nova senha deve ter pelo menos 4 caracteres.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("A confirmação da senha não confere.");
        }

        user.setPassword(newPassword);

        return userRepository.save(user);
    }
}