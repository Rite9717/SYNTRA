package com.project.ims.repository;


import com.project.ims.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    Long countByActiveTrue();
    List<User> findByUsernameContainingOrEmailContainingOrFirstNameContainingOrLastNameContaining(
            String username, String email, String firstName, String lastName);
}
