package com.nus.iss.repository;

import com.nus.iss.entity.ChallengeTemplate;
import com.nus.iss.enums.ChallengeCadence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChallengeTemplateRepository extends JpaRepository<ChallengeTemplate, Long> {
    List<ChallengeTemplate> findByCadenceAndLanguageAndActiveTrueOrderByDisplayOrderAscIdAsc(ChallengeCadence cadence, String language);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
