package com.advent.backend.repository;

import com.advent.backend.entity.MyTteok;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyTteokRepository extends JpaRepository<MyTteok, UUID> {
    Optional<MyTteok> findByMember_Id(UUID memberId);
}
