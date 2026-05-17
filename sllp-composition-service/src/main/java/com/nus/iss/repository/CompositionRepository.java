package com.nus.iss.repository;

import com.nus.iss.entity.Composition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompositionRepository extends JpaRepository<Composition, Long> {

    List<Composition> findByUsername(String username, Pageable pageable);

    Optional<Composition> findByIdAndUsername(Long id, String username);
}
