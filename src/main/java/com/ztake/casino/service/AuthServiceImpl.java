package com.ztake.casino.service;

import com.ztake.casino.model.User;
import com.ztake.casino.repository.UserRepository;
import java.util.Optional;

/**
 * Implementación del servicio de autenticación.
 */
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        // Buscar al usuario por nombre de usuario o email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail);

        // Verificar si existe el usuario y la contraseña es correcta
        if (userOptional.isPresent() && userOptional.get().getPassword().equals(password)) {
            return userOptional;
        }

        return Optional.empty();
    }

    @Override
    public User register(String username, String email, String password) {
        // Verificar si ya existe un usuario con ese nombre o email
        if (userRepository.findByUsernameOrEmail(username).isPresent() ||
                userRepository.findByUsernameOrEmail(email).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario o email ya está en uso");
        }

        // Crear nuevo usuario
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setBalance(0.0); // Balance inicial

        // Guardar y retornar el usuario
        return userRepository.save(newUser);
    }

    @Override
    public boolean recoverPassword(String email) {
        // Buscar al usuario por email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(email);

        // En un sistema real, aquí enviaríamos un email de recuperación
        // Para este ejemplo, solo verificamos si existe el usuario
        return userOptional.isPresent();
    }
}