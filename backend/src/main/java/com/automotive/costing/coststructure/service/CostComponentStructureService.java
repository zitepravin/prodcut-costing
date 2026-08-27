package com.automotive.costing.coststructure.service;

import com.automotive.costing.coststructure.dto.CostStructureDtos.ComponentNode;
import com.automotive.costing.coststructure.dto.CostStructureDtos.ComponentStructureResponse;
import com.automotive.costing.shouldcost.entity.CostComponentDefinition;
import com.automotive.costing.shouldcost.repository.CostComponentDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** A3 — hierarchical cost component structure (extends V92 definitions). */
@Service
@RequiredArgsConstructor
public class CostComponentStructureService {

    private final CostComponentDefinitionRepository repository;

    @Transactional(readOnly = true)
    public ComponentStructureResponse hierarchy() {
        List<CostComponentDefinition> all = repository.findAll().stream()
                .filter(CostComponentDefinition::isActive)
                .sorted(Comparator.comparingInt(CostComponentDefinition::getSequenceNo))
                .toList();

        Map<String, List<CostComponentDefinition>> byParent = all.stream()
                .filter(c -> c.getParentComponentCode() != null && !c.getParentComponentCode().isBlank())
                .collect(Collectors.groupingBy(CostComponentDefinition::getParentComponentCode));

        List<ComponentNode> roots = all.stream()
                .filter(c -> c.getParentComponentCode() == null || c.getParentComponentCode().isBlank())
                .map(c -> toNode(c, byParent))
                .toList();

        return new ComponentStructureResponse(roots);
    }

    private ComponentNode toNode(CostComponentDefinition c, Map<String, List<CostComponentDefinition>> byParent) {
        List<ComponentNode> children = byParent.getOrDefault(c.getComponentCode(), List.of()).stream()
                .sorted(Comparator.comparingInt(CostComponentDefinition::getSequenceNo))
                .map(ch -> toNode(ch, byParent))
                .toList();
        return new ComponentNode(
                c.getComponentCode(),
                c.getDisplayName(),
                c.getCategory(),
                c.getCostBehavior() != null ? c.getCostBehavior() : "VARIABLE",
                c.getHierarchyLevel(),
                c.getSequenceNo(),
                c.getMapsToColumn(),
                children
        );
    }
}
