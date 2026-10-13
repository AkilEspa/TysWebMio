package edu.uclm.esi.tysweb.beusu.dao;

import edu.uclm.esi.tysweb.beusu.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDao extends JpaRepository<User, String> {
    User findByEmail(String email);
    User findByTokenCreacion(String tokenCreacion);
}
