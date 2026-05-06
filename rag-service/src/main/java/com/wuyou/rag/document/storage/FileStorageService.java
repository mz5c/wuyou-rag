package com.wuyou.rag.document.storage;

/**
 * File storage service abstraction.
 * Supports upload, download, delete, and URL retrieval for stored files.
 */
public interface FileStorageService {

    /**
     * Upload a file to storage.
     *
     * @param objectName  unique object key (e.g. "docs/2024/01/uuid.pdf")
     * @param data        file bytes
     * @param contentType MIME type of the file
     * @return the object name used to store the file
     */
    String upload(String objectName, byte[] data, String contentType);

    /**
     * Download a file from storage.
     *
     * @param objectName the object key to retrieve
     * @return file bytes
     */
    byte[] download(String objectName);

    /**
     * Delete a file from storage.
     *
     * @param objectName the object key to remove
     */
    void delete(String objectName);

    /**
     * Get an accessible URL for the file.
     *
     * @param objectName the object key
     * @return URL string that can be used to access the file
     */
    String getUrl(String objectName);
}
