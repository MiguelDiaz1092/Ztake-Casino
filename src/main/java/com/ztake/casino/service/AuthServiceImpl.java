package com.ztake.casino.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.ztake.casino.model.User;
import com.ztake.casino.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implementación mejorada del servicio de autenticación con validaciones robustas.
 */
public class AuthServiceImpl implements AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthServiceImpl.class.getName());
    private static final int DEFAULT_BCRYPT_COST = 12;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty() || password == null) {
            LOGGER.warning("Intento de autenticación con credenciales nulas o vacías");
            return Optional.empty();
        }

        // Buscar al usuario por nombre de usuario o email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail.trim());

        // Verificar si existe el usuario y la contraseña es correcta usando BCrypt
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Verificar si el usuario está activo
            if (!"active".equals(user.getStatus())) {
                LOGGER.warning("Intento de autenticación con usuario inactivo: " + usernameOrEmail);
                return Optional.empty();
            }

            // Verificar la contraseña con BCrypt
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());

            if (result.verified) {
                // Actualizar la fecha de último inicio de sesión
                user.setLastLoginDate(LocalDateTime.now());
                userRepository.save(user);

                LOGGER.info("Usuario autenticado exitosamente: " + user.getUsername());
                return userOptional;
            } else {
                LOGGER.info("Contraseña incorrecta para el usuario: " + usernameOrEmail);
            }
        } else {
            LOGGER.info("Usuario no encontrado: " + usernameOrEmail);
        }

        return Optional.empty();
    }

    @Override
    public User register(String username, String email, String password) {
        // Validar datos con mensajes específicos
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        // Verificar si ya existe un usuario con ese nombre
        if (userRepository.findByUsernameOrEmail(username).isPresent()) {
            LOGGER.warning("Intento de registro con nombre de usuario duplicado: " + username);
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        // Verificar si ya existe un usuario con ese email
        if (userRepository.findByUsernameOrEmail(email).isPresent()) {
            LOGGER.warning("Intento de registro con email duplicado: " + email);
            throw new IllegalArgumentException("El email ya está en uso");
        }

        // Crear nuevo usuario
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setEmail(email.trim().toLowerCase());

        // Hash de la contraseña con BCrypt
        String hashedPassword = BCrypt.withDefaults().hashToString(DEFAULT_BCRYPT_COST, password.toCharArray());
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
        if (email == null || email.trim().isEmpty()) {
            LOGGER.warning("Intento de recuperación de contraseña con email vacío");
            return false;
        }

        // Buscar al usuario por email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(email.trim().toLowerCase());

        // En un sistema real, aquí enviaríamos un email de recuperación
        // Para este ejemplo, solo verificamos si existe el usuario
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Verificar si el usuario está activo
            if (!"active".equals(user.getStatus())) {
                LOGGER.warning("Intento de recuperación de contraseña para usuario inactivo: " + email);
                return false;
            }

            LOGGER.info("Solicitud de recuperación de contraseña generada para: " + email);
            return true;
        }

        LOGGER.info("Intento de recuperación de contraseña fallido para email no registrado: " + email);
        return false;
    }

    @Override
    public boolean changePassword(User user, String currentPassword, String newPassword) {
        if (user == null || currentPassword == null || newPassword == null) {
            LOGGER.warning("Intento de cambio de contraseña con datos nulos");
            return false;
        }

        // Verificar la contraseña actual
        BCrypt.Result result = BCrypt.verifyer().verify(currentPassword.toCharArray(), user.getPassword());

        if (!result.verified) {
            LOGGER.info("Intento de cambio de contraseña fallido para: " + user.getUsername() + " (contraseña actual incorrecta)");
            return false;
        }

        // Validar la nueva contraseña
        try {
            validatePassword(newPassword);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Nueva contraseña inválida para usuario: " + user.getUsername() + " - " + e.getMessage());
            throw e; // Re-lanzar la excepción para que la UI pueda manejarla
        }

        // Hash de la nueva contraseña
        String hashedNewPassword = BCrypt.withDefaults().hashToString(DEFAULT_BCRYPT_COST, newPassword.toCharArray());
        user.setPassword(hashedNewPassword);

        // Guardar los cambios
        userRepository.save(user);

        LOGGER.info("Contraseña cambiada exitosamente para el usuario: " + user.getUsername());
        return true;
    }

    // Métodos de validación mejorados

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        String trimmedUsername = username.trim();

        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 3 y 50 caracteres");
        }

        // Validar que solo contiene caracteres alfanuméricos y guiones bajos
        if (!USERNAME_PATTERN.matcher(trimmedUsername).matches()) {
            throw new IllegalArgumentException("El nombre de usuario solo puede contener letras, números y guiones bajos");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        String trimmedEmail = email.trim().toLowerCase();

        // Validación de formato de email con regex más robusta
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
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

        if (password.length() > 100) {
            throw new IllegalArgumentException("La contraseña no puede exceder los 100 caracteres");
        }

        // Validaciones de seguridad mejoradas
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasUppercase = false;
        boolean hasLowercase = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (Character.isUpperCase(c)) {
                    hasUppercase = true;
                } else {
                    hasLowercase = true;
                }
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSpecial = true;
            }
        }

        // Requisitos mínimos (solo se exige número y letra)
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("La contraseña debe contener al menos un número y una letra");
        }

        // Sugerencias para contraseñas más fuertes (no obligatorias)
        if (!hasSpecial || !hasUppercase || !hasLowercase) {
            LOGGER.info("Recomendación: La contraseña sería más segura con mayúsculas, minúsculas y caracteres especiales");
        }
    }
}