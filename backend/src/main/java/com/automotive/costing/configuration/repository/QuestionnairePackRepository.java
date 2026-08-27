package com.automotive.costing.configuration.repository;

import com.automotive.costing.configuration.entity.QuestionnairePack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnairePackRepository extends JpaRepository<QuestionnairePack, String> {
    List<QuestionnairePack> findAllByOrderByPackCodeAscVersionNoAsc();
    Optional<QuestionnairePack> findByPackCodeAndVersionNo(String packCode, int versionNo);
    long countByStatus(String status);
}
