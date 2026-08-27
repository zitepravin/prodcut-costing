package com.automotive.costing.helpingagent.controller;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatRequest;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatResponse;
import com.automotive.costing.helpingagent.service.HelpingAgentService;
import com.automotive.costing.platform.access.RequiresModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/helping-agent")
@RequiresModule("PRODUCT_COSTING")
@RequiredArgsConstructor
public class HelpingAgentController {
    private final HelpingAgentService helpingAgentService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(helpingAgentService.chat(request));
    }
}
