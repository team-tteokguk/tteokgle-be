package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // 소셜 타입과 소셜 아이디 조합으로 유저 검색
    Optional<User> findBySocialIdAndSocialType(String socialId, Member.SocialType socialType);
}
