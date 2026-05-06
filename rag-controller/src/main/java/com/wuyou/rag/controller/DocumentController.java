package com.wuyou.rag.controller;

import com.wuyou.rag.document.DocumentService;
import com.wuyou.rag.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public Result<Void> upload(@RequestParam Long kbId,
                                @RequestParam("file") MultipartFile file,
                                @AuthenticationPrincipal Long userId) {
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            return Result.fail(5002, "文件大小超过50MB限制");
        }
        return documentService.upload(kbId, file, userId);
    }

    @GetMapping
    @Operation(summary = "获取知识库文档列表")
    public Result<?> list(@RequestParam Long kbId,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int size) {
        return documentService.pageByKbId(kbId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情")
    public Result<?> getById(@PathVariable Long id) {
        return documentService.getById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public Result<Void> delete(@PathVariable Long id) {
        return documentService.delete(id);
    }

    @GetMapping("/{id}/process-status")
    @Operation(summary = "获取文档处理状态")
    public Result<Integer> getProcessStatus(@PathVariable Long id) {
        return documentService.getProcessStatus(id);
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "重新处理文档")
    public Result<Void> reprocess(@PathVariable Long id) {
        return documentService.reprocess(id);
    }
}
