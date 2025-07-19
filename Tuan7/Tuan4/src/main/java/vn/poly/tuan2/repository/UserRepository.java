package vn.poly.tuan2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.poly.tuan2.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
