package com.wuyou.rag.controller;

import com.wuyou.rag.document.KnowledgeBaseService;
import com.wuyou.rag.entity.kb.KbKnowledgeBase;
import com.wuyou.rag.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "知识库管理")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public Result<List<KbKnowledgeBase>> list() {
        return knowledgeBaseService.listAll();
    }

    @GetMapping("/{id}")
    public Result<KbKnowledgeBase> getById(@PathVariable Long id) {
        return knowledgeBaseService.getById(id);
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateKnowledgeBaseRequest request,
                               @AuthenticationPrincipal Long userId) {
        return knowledgeBaseService.create(request.getName(), request.getDescription(), userId);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        return knowledgeBaseService.update(id, request.getName(), request.getDescription());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return knowledgeBaseService.delete(id);
    }

    @Data
    public static class CreateKnowledgeBaseRequest {
        @NotBlank(message = "知识库名称不能为空")
        private String name;

        private String description;
    }

    @Data
    public static class UpdateKnowledgeBaseRequest {
        @NotBlank(message = "知识库名称不能为空")
        private String name;

        private String description;
    }
}
