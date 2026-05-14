package com.wuyou.rag.document.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.rag.config.RabbitConfig;
import com.wuyou.rag.document.DocumentService;
import com.wuyou.rag.document.storage.FileStorageService;
import com.wuyou.rag.entity.kb.KbChunk;
import com.wuyou.rag.entity.kb.KbDocument;
import com.wuyou.rag.entity.kb.KbKnowledgeBase;
import com.wuyou.rag.entity.sys.SysUser;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbChunkMapper;
import com.wuyou.rag.mapper.KbDocumentMapper;
import com.wuyou.rag.mapper.KbKnowledgeBaseMapper;
import com.wuyou.rag.mapper.SysUserMapper;
import com.wuyou.rag.rag.vector.VectorService;
import com.wuyou.rag.result.Result;
import com.wuyou.rag.rag.search.EsSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "docx", "md");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB

    private final KbDocumentMapper kbDocumentMapper;
    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    private final KbChunkMapper kbChunkMapper;
    private final SysUserMapper sysUserMapper;
    private final FileStorageService fileStorageService;
    private final VectorService vectorService;
    private final EsSearchService esSearchService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Result<Void> upload(Long kbId, MultipartFile file, Long userId) {
        // Validate knowledge base exists
        KbKnowledgeBase kb = kbKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "知识库不存在");
        }

        // Validate file not empty
        if (file.isEmpty()) {
            return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "文件不能为空");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.fail(ErrorCode.FILE_TOO_LARGE.getCode(), "文件大小超过50MB限制");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }

        // Determine file type from extension
        String fileType = getFileExtension(originalFilename);
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            return Result.fail(ErrorCode.FILE_PARSE_ERROR.getCode(),
                    "不支持的文件类型: " + fileType + "，仅支持 pdf/docx/md");
        }

        try {
            // Get file bytes and content type
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = getContentTypeByExtension(fileType);
            }

            // Generate object name: docs/{kbId}/{uuid}-{originalName}
            String objectName = "docs/" + kbId + "/" + UUID.randomUUID() + "-" + originalFilename;

            // Upload to MinIO
            fileStorageService.upload(objectName, fileBytes, contentType);
            log.info("File uploaded to MinIO: objectName={}, kbId={}, userId={}", objectName, kbId, userId);

            // Create document record
            KbDocument document = new KbDocument();
            document.setKbId(kbId);
            document.setTitle(originalFilename);
            document.setFileType(fileType);
            document.setFileUrl(objectName);
            document.setFileSize(file.getSize());
            document.setStatus(0); // PENDING
            document.setCreateBy(userId);
            kbDocumentMapper.insert(document);

            // Send docId to MQ for async processing
            rabbitTemplate.convertAndSend(RabbitConfig.DOCUMENT_PROCESS_QUEUE, document.getId());
            log.info("Document processing message sent: docId={}", document.getId());

            return Result.success(null);

        } catch (Exception e) {
            log.error("File upload failed: filename={}", originalFilename, e);
            return Result.fail(ErrorCode.FILE_PARSE_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> pageByKbId(Long kbId, int page, int size) {
        Page<KbDocument> pageResult = kbDocumentMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<KbDocument>lambdaQuery()
                        .eq(KbDocument::getKbId, kbId)
                        .orderByDesc(KbDocument::getCreateTime));

        // Batch query creator names
        List<KbDocument> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            Set<Long> userIds = records.stream()
                    .map(KbDocument::getCreateBy)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
                Map<Long, String> userNames = new HashMap<>();
                for (SysUser u : users) {
                    userNames.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
                }
                for (KbDocument doc : records) {
                    doc.setCreatorName(userNames.get(doc.getCreateBy()));
                }
            }
        }

        return Result.success(pageResult);
    }

    @Override
    public Result<?> getById(Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        return Result.success(document);
    }

    @Override
    public Result<Void> delete(Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }

        // Delete from MinIO
        try {
            fileStorageService.delete(document.getFileUrl());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: url={}", document.getFileUrl(), e);
        }

        // Delete vectors from Milvus
        try {
            vectorService.deleteByDocId(id);
        } catch (Exception e) {
            log.warn("Failed to delete vectors from Milvus: docId={}", id, e);
        }

        // Delete from ES
        try {
            esSearchService.deleteByDocId(id);
        } catch (Exception e) {
            log.warn("Failed to delete from ES: docId={}", id, e);
        }

        // Delete chunks from DB
        kbChunkMapper.delete(
                Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, id));

        // Logic delete document
        kbDocumentMapper.deleteById(id);
        log.info("Document deleted: id={}, title={}", id, document.getTitle());

        return Result.success(null);
    }

    @Override
    public Result<Integer> getProcessStatus(Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        return Result.success(document.getStatus());
    }

    @Override
    public Result<Void> reprocess(Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }

        // Delete old chunks
        List<KbChunk> oldChunks = kbChunkMapper.selectList(
                Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, id));
        if (!oldChunks.isEmpty()) {
            kbChunkMapper.delete(
                    Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, id));
        }

        // Delete old vectors from Milvus
        try {
            vectorService.deleteByDocId(id);
        } catch (Exception e) {
            log.warn("Failed to delete vectors from Milvus: docId={}", id, e);
        }

        // Delete from ES
        try {
            esSearchService.deleteByDocId(id);
        } catch (Exception e) {
            log.warn("Failed to delete from ES during reprocess: docId={}", id, e);
        }

        // Reset document status
        KbDocument updateDoc = new KbDocument();
        updateDoc.setId(id);
        updateDoc.setStatus(0);
        updateDoc.setErrorMsg(null);
        updateDoc.setChunkCount(0);
        kbDocumentMapper.updateById(updateDoc);

        // Re-send MQ message
        rabbitTemplate.convertAndSend(RabbitConfig.DOCUMENT_PROCESS_QUEUE, id);
        log.info("Document reprocess triggered: docId={}", id);

        return Result.success(null);
    }

    /**
     * Extract file extension from filename (lowercase).
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Map file extension to MIME content type.
     */
    private String getContentTypeByExtension(String fileType) {
        return switch (fileType) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "md" -> "text/markdown";
            default -> "application/octet-stream";
        };
    }
}
