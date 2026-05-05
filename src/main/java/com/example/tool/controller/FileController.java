package com.example.tool.controller;

import com.example.tool.entity.FileMetadata;
import com.example.tool.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "File Management", description = "APIs for uploading and downloading compliance-related files")
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "Upload a file",
            description = "Uploads a file (PDF, DOCX, PNG, JPG). Max size 10MB. Requires ROLE_ADMIN or ROLE_MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file type, empty file, or file too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @Parameter(description = "File to upload (PDF, DOCX, PNG, JPG — max 10MB)")
            @RequestParam("file") MultipartFile file) throws IOException {
        FileMetadata metadata = fileService.upload(file);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/{id}")
                .buildAndExpand(metadata.getId())
                .toUri();

        return ResponseEntity.created(location).body(Map.of(
                "id",           metadata.getId(),
                "originalName", metadata.getOriginalName(),
                "fileType",     metadata.getFileType(),
                "size",         metadata.getSize(),
                "uploadedAt",   metadata.getUploadedAt().toString(),
                "downloadUrl",  location.toString()
        ));
    }

    @Operation(
            summary = "Download a file by ID",
            description = "Downloads a previously uploaded file by its metadata ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
            @Parameter(description = "ID of the file metadata record", example = "1")
            @PathVariable Long id) throws IOException {
        FileMetadata metadata = fileService.getMetadata(id);
        Resource resource    = fileService.download(id);

        String sanitizedFilename = metadata.getOriginalName()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + sanitizedFilename + "\"")
                .body(resource);
    }
}
