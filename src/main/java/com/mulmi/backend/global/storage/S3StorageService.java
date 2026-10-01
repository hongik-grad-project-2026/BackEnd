package com.mulmi.backend.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final Pattern FILE_EXTENSION_PATTERN =
            Pattern.compile("\\.([a-zA-Z0-9]{1,10})$");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public String upload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 필요합니다.");
        }

        String objectKey = createObjectKey(directory, file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(Optional.ofNullable(file.getContentType())
                        .orElse("application/octet-stream"))
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException exception) {
            throw new IllegalStateException("파일을 읽을 수 없습니다.", exception);
        }

        return objectKey;
    }

    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        s3Client.deleteObject(request);
    }

    public String createDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String createObjectKey(String directory, String originalFilename) {
        String extension = extractExtension(originalFilename);
        String filename = UUID.randomUUID() + extension;

        if (directory == null || directory.isBlank()) {
            return filename;
        }

        String normalizedDirectory = directory.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (normalizedDirectory.contains("..")) {
            throw new IllegalArgumentException("올바르지 않은 파일 저장 경로입니다.");
        }
        return normalizedDirectory + "/" + filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(originalFilename);
        if (!matcher.find()) {
            return "";
        }

        return "." + matcher.group(1).toLowerCase(Locale.ROOT);
    }
}
