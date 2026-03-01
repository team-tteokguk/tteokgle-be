package com.advent.backend.repository;

import com.advent.backend.entity.ShareLink;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    void deleteAllByStoreId(UUID storeId);
}
