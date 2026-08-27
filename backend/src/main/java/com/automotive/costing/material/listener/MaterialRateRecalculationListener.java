package com.automotive.costing.material.listener;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.event.MaterialRatePublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MAT-05: "System shall recalculate all dependent BOM and part costs automatically when
 * a material master rate is updated and published." Listens for the event
 * {@code MaterialMasterService} publishes on every rate-version publish, finds every
 * {@code BomNode} linked to that material code, and rolls up each affected program.
 *
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} deliberately runs this AFTER
 * the rate-publish transaction commits - recalculating BOM costs against a rate that
 * might still roll back would produce a roll-up that never actually goes live, and would
 * also read the new rate through a separate transaction/connection before it's visible.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaterialRateRecalculationListener {

    private final BomNodeRepository bomNodeRepository;
    private final BomNodeService bomNodeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaterialRatePublished(MaterialRatePublishedEvent event) {
        List<BomNode> affected = bomNodeRepository.findByMaterialCode(event.materialCode());
        if (affected.isEmpty()) {
            return;
        }

        Set<String> programIds = affected.stream().map(BomNode::getProgramId).collect(Collectors.toSet());
        log.info("MAT-05: material '{}' rate published (record {}); recalculating {} BOM node(s) "
                        + "across {} program(s).",
                event.materialCode(), event.recordId(), affected.size(), programIds.size());

        for (String programId : programIds) {
            bomNodeService.triggerRollupForProgram(programId);
        }
    }
}
