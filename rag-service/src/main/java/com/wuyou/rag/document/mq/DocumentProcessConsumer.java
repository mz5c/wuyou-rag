package com.wuyou.rag.document.mq;

import com.wuyou.rag.config.RabbitConfig;
import com.wuyou.rag.document.chunk.ChunkResult;
import com.wuyou.rag.document.chunk.TextChunker;
import com.wuyou.rag.document.parser.DocumentParser;
import com.wuyou.rag.document.storage.FileStorageService;
import com.wuyou.rag.entity.kb.KbChunk;
import com.wuyou.rag.entity.kb.KbDocument;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbChunkMapper;
import com.wuyou.rag.mapper.KbDocumentMapper;
import com.wuyou.rag.rag.embedding.EmbeddingService;
import com.wuyou.rag.rag.vector.VectorService;
import com.wuyou.rag.rag.search.EsSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessConsumer {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final FileStorageService fileStorageService;
    private final List<DocumentParser> documentParsers;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final EsSearchService esSearchService;

    @RabbitListener(queues = RabbitConfig.DOCUMENT_PROCESS_QUEUE)
    public void handleDocumentProcess(Long docId) {
        log.info("Processing document: docId={}", docId);

        KbDocument document = kbDocumentMapper.selectById(docId);
        if (document == null) {
            log.warn("Document not found: docId={}", docId);
            return;
        }

        // Update status to PROCESSING (1)
        KbDocument processingUpdate = new KbDocument();
        processingUpdate.setId(docId);
        processingUpdate.setStatus(1);
        kbDocumentMapper.updateById(processingUpdate);

        try {
            // Download file from MinIO
            byte[] fileData = fileStorageService.download(document.getFileUrl());

            // Find matching parser by fileType
            DocumentParser parser = documentParsers.stream()
                    .filter(p -> p.supportedType().equals(document.getFileType()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.FILE_PARSE_ERROR,
                            "Unsupported file type: " + document.getFileType()));

            // Parse text
            String text = parser.parse(fileData, document.getTitle());
            log.info("Document parsed: docId={}, textLength={}", docId, text.length());

            // Chunk text
            List<ChunkResult> chunks = textChunker.chunk(text);
            log.info("Document chunked: docId={}, chunkCount={}", docId, chunks.size());

            // Process each chunk: embed and persist
            List<KbChunk> chunkEntities = new ArrayList<>();
            List<Long> chunkIds = new ArrayList<>();
            List<String> contents = new ArrayList<>();

            for (ChunkResult chunk : chunks) {
                contents.add(chunk.content());

                // Insert chunk entity to kb_chunk table
                KbChunk chunkEntity = new KbChunk();
                chunkEntity.setDocId(docId);
                chunkEntity.setChunkContent(chunk.content());
                chunkEntity.setChunkIndex(chunk.index());
                chunkEntity.setChunkSize(chunk.size());
                kbChunkMapper.insert(chunkEntity);

                chunkIds.add(chunkEntity.getId());
                chunkEntities.add(chunkEntity);
            }

            // Embed chunk contents
            List<float[]> vectors = embeddingService.embed(contents);

            // Batch insert vectors to Milvus
            vectorService.insertVectors(chunkIds, vectors, document.getKbId());
            log.info("Vectors inserted to Milvus: docId={}, vectorCount={}", docId, chunkIds.size());

            // Index chunks to ES
            try {
                for (int i = 0; i < chunkEntities.size(); i++) {
                    KbChunk chunk = chunkEntities.get(i);
                    esSearchService.indexChunk(chunk.getId(), docId, chunk.getChunkContent(), document.getKbId());
                }
                log.info("Chunks indexed to ES: docId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to index chunks to ES, docId={}", docId, e);
            }

            // Update chunk vectorId with chunk ID as reference
            for (KbChunk chunk : chunkEntities) {
                chunk.setVectorId(String.valueOf(chunk.getId()));
                kbChunkMapper.updateById(chunk);
            }

            // Update document status to COMPLETED (2)
            KbDocument completedUpdate = new KbDocument();
            completedUpdate.setId(docId);
            completedUpdate.setStatus(2);
            completedUpdate.setChunkCount(chunks.size());
            completedUpdate.setErrorMsg(null);
            kbDocumentMapper.updateById(completedUpdate);

            log.info("Document processing completed: docId={}, chunks={}", docId, chunks.size());

        } catch (Exception e) {
            log.error("Document processing failed: docId={}", docId, e);

            KbDocument failedUpdate = new KbDocument();
            failedUpdate.setId(docId);
            failedUpdate.setStatus(3);
            String errorMsg = e.getMessage();
            failedUpdate.setErrorMsg(errorMsg != null ? errorMsg.substring(0, Math.min(errorMsg.length(), 500)) : "Unknown error");
            kbDocumentMapper.updateById(failedUpdate);
        }
    }
}
