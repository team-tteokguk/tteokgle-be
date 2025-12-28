package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class UserRepository {
    @PersistenceContext
    private EntityManager em;

    public void save(Member member) {
        em.persist(member);
    }

    public Member findOne(UUID id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select u from Member u", Member.class)
                .getResultList();
    }
}
