package com.automotive.costing.lifecycle.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.CurveResponse;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.UpsertCurveRequest;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.YearResponse;
import com.automotive.costing.lifecycle.entity.ProgramVolumeCurve;
import com.automotive.costing.lifecycle.entity.ProgramVolumeCurveYear;
import com.automotive.costing.lifecycle.entity.VolumeCurveStatus;
import com.automotive.costing.lifecycle.repository.ProgramVolumeCurveRepository;
import com.automotive.costing.lifecycle.repository.ProgramVolumeCurveYearRepository;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolumeCurveService {

    private final ProgramVolumeCurveRepository curveRepository;
    private final ProgramVolumeCurveYearRepository yearRepository;
    private final AccessScopeService accessScopeService;

    @Transactional(readOnly = true)
    public List<CurveResponse> list(String programId) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        return curveRepository.findByProgramIdOrderByScenarioCodeAsc(programId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CurveResponse get(String programId, String scenarioCode) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        ProgramVolumeCurve curve = curveRepository
                .findByProgramIdAndScenarioCodeAndStatus(programId, scenarioCode, VolumeCurveStatus.ACTIVE)
                .or(() -> curveRepository.findFirstByProgramIdAndScenarioCodeOrderByCreatedAtDesc(programId, scenarioCode))
                .orElseThrow(() -> ResourceNotFoundException.of("ProgramVolumeCurve", programId + "/" + scenarioCode));
        return toResponse(curve);
    }

    @Transactional
    public CurveResponse upsert(String programId, String scenarioCode, UpsertCurveRequest request) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        String code = scenarioCode.trim().toUpperCase();

        ProgramVolumeCurve curve = curveRepository
                .findFirstByProgramIdAndScenarioCodeOrderByCreatedAtDesc(programId, code)
                .orElseGet(() -> {
                    ProgramVolumeCurve c = new ProgramVolumeCurve();
                    c.setCurveId(UUID.randomUUID().toString());
                    c.setProgramId(programId);
                    c.setScenarioCode(code);
                    c.setStatus(VolumeCurveStatus.DRAFT);
                    return c;
                });
        curve.setScenarioLabel(request.scenarioLabel() != null ? request.scenarioLabel() : code);
        curve.setNotes(request.notes());
        curveRepository.save(curve);

        yearRepository.deleteByCurveId(curve.getCurveId());
        for (var y : request.years()) {
            ProgramVolumeCurveYear row = new ProgramVolumeCurveYear();
            row.setCurveId(curve.getCurveId());
            row.setYearIndex(y.yearIndex());
            row.setCalendarYear(y.calendarYear());
            row.setPlannedVolume(y.plannedVolume());
            yearRepository.save(row);
        }
        return toResponse(curve);
    }

    @Transactional
    public CurveResponse activate(String programId, String scenarioCode) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        String code = scenarioCode.trim().toUpperCase();
        ProgramVolumeCurve curve = curveRepository
                .findFirstByProgramIdAndScenarioCodeOrderByCreatedAtDesc(programId, code)
                .orElseThrow(() -> ResourceNotFoundException.of("ProgramVolumeCurve", programId + "/" + code));

        // Archive other ACTIVE curves for same scenario
        curveRepository.findByProgramIdAndScenarioCodeAndStatus(programId, code, VolumeCurveStatus.ACTIVE)
                .ifPresent(existing -> {
                    if (!existing.getCurveId().equals(curve.getCurveId())) {
                        existing.setStatus(VolumeCurveStatus.ARCHIVED);
                        curveRepository.save(existing);
                    }
                });
        curve.setStatus(VolumeCurveStatus.ACTIVE);
        curveRepository.save(curve);
        return toResponse(curve);
    }

    private CurveResponse toResponse(ProgramVolumeCurve curve) {
        List<YearResponse> years = yearRepository.findByCurveIdOrderByYearIndexAsc(curve.getCurveId()).stream()
                .map(y -> new YearResponse(y.getYearIndex(), y.getCalendarYear(), y.getPlannedVolume()))
                .toList();
        return new CurveResponse(
                curve.getCurveId(),
                curve.getProgramId(),
                curve.getScenarioCode(),
                curve.getScenarioLabel(),
                curve.getStatus().name(),
                curve.getNotes(),
                years
        );
    }
}
