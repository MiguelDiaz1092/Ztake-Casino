package com.ztake.casino.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.ztake.casino.model.User;
import com.ztake.casino.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación mejorada del servicio de autenticación.
 */
public class AuthServiceImpl implements AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthServiceImpl.class.getName());
    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        // Buscar al usuario por nombre de usuario o email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail);

        // Verificar si existe el usuario y la contraseña es correcta usando BCrypt
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Verificar la contraseña con BCrypt
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());

            if (result.verified) {
                // Actualizar la fecha de último inicio de sesión
                user.setLastLoginDate(LocalDateTime.now());
                userRepository.save(user);

                LOGGER.info("Usuario autenticado: " + user.getUsername());
                return userOptional;
            }
        }

        LOGGER.info("Intento de autenticación fallido para: " + usernameOrEmail);
        return Optional.empty();
    }

    @Override
    public User register(String username, String email, String password) {
        // Verificar si ya existe un usuario con ese nombre o email
        if (userRepository.findByUsernameOrEmail(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        if (userRepository.findByUsernameOrEmail(email).isPresent()) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        // Validar datos
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        // Crear nuevo usuario
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);

        // Hash de la contraseña con BCrypt
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        newUser.setPassword(hashedPassword);

        newUser.setBalance(0.0); // Balance inicial
        newUser.setRegistrationDate(LocalDateTime.now());
        newUser.setStatus("active");

        // Guardar y retornar el usuario
        User savedUser = userRepository.save(newUser);
        LOGGER.info("Nuevo usuario registrado: " + savedUser.getUsername());
        return savedUser;
    }

    @Override
    public boolean recoverPassword(String email) {
        // Buscar al usuario por email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(email);

        // En un sistema real, aquí enviaríamos un email de recuperación
        // Para este ejemplo, solo verificamos si existe el usuario
        if (userOptional.isPresent()) {
            LOGGER.info("Solicitud de recuperación de contraseña para: " + email);
            return true;
        }

        LOGGER.info("Intento de recuperación de contraseña fallido para: " + email);
        return false;
    }

    @Override
    public boolean changePassword(User user, String currentPassword, String newPassword) {
        // Verificar la contraseña actual
        BCrypt.Result result = BCrypt.verifyer().verify(currentPassword.toCharArray(), user.getPassword());

        if (!result.verified) {
            LOGGER.info("Intento de cambio de contraseña fallido para: " + user.getUsername() + " (contraseña actual incorrecta)");
            return false;
        }

        // Validar la nueva contraseña
        validatePassword(newPassword);

        // Hash de la nueva contraseña
        String hashedNewPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        user.setPassword(hashedNewPassword);

        // Guardar los cambios
        userRepository.save(user);

        LOGGER.info("Contraseña cambiada para el usuario: " + user.getUsername());
        return true;
    }

    // Métodos de validación

    private void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 3 y 50 caracteres");
        }

        // Validar que solo contiene caracteres alfanuméricos y guiones bajos
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("El nombre de usuario solo puede contener letras, números y guiones bajos");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        // Validación simple de formato de email
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }

        // Validar que la contraseña tiene al menos un número y una letra
        if (!password.matches(".*[0-9].*") || !password.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("La contraseña debe contener al menos un número y una letra");
        }
    }
}