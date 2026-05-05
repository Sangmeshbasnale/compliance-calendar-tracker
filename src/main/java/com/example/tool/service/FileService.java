package com.example.tool.service;

import com.example.tool.entity.FileMetadata;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.exception.ResourceNotFoundException;
import com.example.tool.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg"
    );
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final FileMetadataRepository fileMetadataRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        log.info("File upload directory: {}", uploadPath);
    }

    public FileMetadata upload(MultipartFile file) throws IOException {
        validateFile(file);

        String extension  = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        Path   targetPath = uploadPath.resolve(storedName).normalize();

        // Path traversal guard — ensure resolved path stays within upload directory
        if (!targetPath.startsWith(uploadPath)) {
            throw new InvalidDataException("Invalid file path detected");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File stored: {}", targetPath);

        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalName(file.getOriginalFilename());
        metadata.setStoredName(storedName);
        metadata.setFilePath(targetPath.toString());
        metadata.setFileType(file.getContentType());
        metadata.setSize(file.getSize());

        return fileMetadataRepository.save(metadata);
    }

    public Resource download(Long id) throws MalformedURLException {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + id));

        Path filePath = Paths.get(metadata.getFilePath()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("File not found on disk: " + metadata.getStoredName());
        }
        return resource;
    }

    public FileMetadata getMetadata(Long id) {
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + id));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDataException("File must not be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidDataException("File size exceeds maximum allowed size of 10 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidDataException("File type not allowed. Allowed types: PDF, DOCX, PNG, JPG");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
