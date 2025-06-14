package com.riskengine.risksystem.repository;

import com.riskengine.risksystem.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    UserProfile findByUsername(String username);
    UserProfile findByEmail(String email);
}