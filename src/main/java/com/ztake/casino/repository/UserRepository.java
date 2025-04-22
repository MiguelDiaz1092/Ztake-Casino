package com.ztake.casino.repository;

import com.ztake.casino.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de usuarios.
 * Define las operaciones básicas de acceso a datos para los usuarios.
 */
public interface UserRepository {

    /**
     * Busca un usuario por su nombre de usuario o email.
     *
     * @param usernameOrEmail nombre de usuario o email del usuario
     * @return Optional con el usuario si existe, Optional vacío si no
     */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    /**
     * Guarda un nuevo usuario o actualiza uno existente.
     *
     * @param user usuario a guardar o actualizar
     * @return usuario guardado
     */
    User save(User user);

    /**
     * Obtiene todos los usuarios.
     *
     * @return lista de usuarios
     */
    List<User> findAll();

    /**
     * Busca un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Optional con el usuario si existe, Optional vacío si no
     */
    Optional<User> findById(Long id);

    /**
     * Elimina un usuario por su ID.
     *
     * @param id ID del usuario a eliminar
     */
    void deleteById(Long id);
}