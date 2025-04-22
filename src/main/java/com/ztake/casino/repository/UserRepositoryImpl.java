package com.ztake.casino.repository;

import com.ztake.casino.model.User;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del repositorio de usuarios.
 * Para un proyecto real, esto se conectaría a una base de datos.
 */
public class UserRepositoryImpl implements UserRepository {

    // Simulación de una base de datos en memoria
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    public UserRepositoryImpl() {
        // Inicializar con algunos usuarios de prueba
        save(new User(null, "admin", "admin@ztake.com", "admin123", 10000.0));
        save(new User(null, "user", "user@ztake.com", "user123", 1000.0));
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return users.values().stream()
                .filter(user -> user.getUsername().equals(usernameOrEmail) ||
                        user.getEmail().equals(usernameOrEmail))
                .findFirst();
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            // Es un nuevo usuario, asignar ID
            user.setId(nextId++);
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void deleteById(Long id) {
        users.remove(id);
    }
}