package com.automotive.costing.configuration.repository;

import com.automotive.costing.configuration.entity.QuestionnairePackItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionnairePackItemRepository extends JpaRepository<QuestionnairePackItem, String> {
    List<QuestionnairePackItem> findByPackIdOrderBySortOrderAsc(String packId);
    void deleteByPackId(String packId);
}
