package com.example.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestFileService {

    private final RequestFileRepository requestFileRepository;

    public void saveFile(Long requestId, String fileName, byte[] fileData) {
        if (fileData.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Dosya boyutu 10 MB'ı aşamaz.");
        }

        RequestFile file = new RequestFile();
        file.setRequestId(requestId);
        file.setFileName(fileName);
        file.setFileData(fileData);
        file.setFileSize((long) fileData.length);

        requestFileRepository.save(file);
    }

    public List<RequestFile> getFilesByRequestId(Long requestId) {
        return requestFileRepository.findByRequestId(requestId);
    }

    public RequestFile getFileById(Long fileId) {
        return requestFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı."));
    }
}
