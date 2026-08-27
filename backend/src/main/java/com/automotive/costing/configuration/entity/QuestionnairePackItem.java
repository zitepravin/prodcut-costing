package com.automotive.costing.configuration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questionnaire_pack_item")
public class QuestionnairePackItem {
    @Id
    @Column(name = "item_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String itemId;
    @Column(name = "pack_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String packId;
    @Column(name = "question_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String questionId;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
