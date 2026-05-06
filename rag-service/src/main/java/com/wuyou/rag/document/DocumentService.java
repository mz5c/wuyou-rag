package com.wuyou.rag.document;

import com.wuyou.rag.result.Result;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    Result<Void> upload(Long kbId, MultipartFile file, Long userId);

    Result<?> pageByKbId(Long kbId, int page, int size);

    Result<?> getById(Long id);

    Result<Void> delete(Long id);

    Result<Integer> getProcessStatus(Long id);

    Result<Void> reprocess(Long id);
}
