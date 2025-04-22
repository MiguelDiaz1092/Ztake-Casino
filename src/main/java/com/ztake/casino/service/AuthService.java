package com.ztake.casino.service;

import com.ztake.casino.model.User;
import java.util.Optional;

/**
 * Interfaz para el servicio de autenticación.
 */
public interface AuthService {

    /**
     * Intenta autenticar a un usuario con sus credenciales.
     *
     * @param usernameOrEmail nombre de usuario o email
     * @param password contraseña
     * @return Optional con el usuario autenticado si es exitoso, Optional vacío si no
     */
    Optional<User> authenticate(String usernameOrEmail, String password);

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param username nombre de usuario
     * @param email email
     * @param password contraseña
     * @return usuario registrado
     */
    User register(String username, String email, String password);

    /**
     * Recupera la contraseña de un usuario.
     *
     * @param email email del usuario
     * @return true si se pudo enviar el correo de recuperación, false si no
     */
    boolean recoverPassword(String email);
}