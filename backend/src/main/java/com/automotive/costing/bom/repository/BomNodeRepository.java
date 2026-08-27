package com.automotive.costing.bom.repository;

import com.automotive.costing.bom.entity.BomNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TDD Section 6.3: "avoid N+1 database queries during roll-up: batch-load all nodes and
 * their current cost records for the subtree in one query, then perform the roll-up in
 * memory." findSubtree() and findFullTreeByProgram() are the two batch-load entry points
 * every roll-up operation should use instead of walking parent/child relations one query
 * at a time.
 *
 * Native WITH RECURSIVE CTEs are used because MySQL 8 supports them and Spring Data JPA
 * has no portable abstraction for recursive queries; JPQL cannot express this.
 */
@Repository
public interface BomNodeRepository extends JpaRepository<BomNode, String> {

    Optional<BomNode> findByBomNodeId(String bomNodeId);

    List<BomNode> findByParentNodeId(String parentNodeId);

    long countByProgramId(String programId);

    /**
     * Returns every node in the given node's subtree (itself + all descendants at any
     * depth), in a single query. This is the batch-load TDD Section 6.3 calls for -
     * callers should load once here and perform roll-up entirely in memory rather than
     * issuing one query per level.
     */
    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT * FROM bom_node WHERE bom_node_id = :rootNodeId
                UNION ALL
                SELECT b.* FROM bom_node b
                INNER JOIN subtree s ON b.parent_node_id = s.bom_node_id
            )
            SELECT * FROM subtree
            """, nativeQuery = true)
    List<BomNode> findSubtree(@Param("rootNodeId") String rootNodeId);

    /**
     * Returns the full BOM tree for a program (every node, regardless of depth) in one
     * query, for the GET /api/v1/bom/{programId}/tree endpoint and for a full-program
     * roll-up. At 20,000 nodes (SRS NFR 5.1's scale target), this single query plus an
     * in-memory topological roll-up is what keeps the 30-second roll-up SLA achievable.
     */
    @Query(value = "SELECT * FROM bom_node WHERE program_id = :programId", nativeQuery = true)
    List<BomNode> findFullTreeByProgram(@Param("programId") String programId);

    /** Root nodes (no parent) for a program - typically exactly one per program. */
    List<BomNode> findByProgramIdAndParentNodeIdIsNull(String programId);

    /**
     * MAT-05: "recalculate all dependent BOM and part costs automatically when a material
     * master rate is updated and published." This is the lookup that finds every leaf node
     * a material rate change affects, so MaterialRateRecalculationListener knows what to
     * recompute and which program(s)' roll-ups to re-trigger.
     */
    List<BomNode> findByMaterialCode(String materialCode);

    // --- Tenant-aware accessors (Wave 1 multi-tenancy) ---

    Optional<BomNode> findByCustomerIdAndBomNodeId(String customerId, String bomNodeId);

    List<BomNode> findByCustomerIdAndProgramId(String customerId, String programId);

    long countByCustomerIdAndProgramId(String customerId, String programId);

    long countByCustomerId(String customerId);
}
