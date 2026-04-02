package com.userservice.pantry.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PantryImageStorageService {

    private final S3Client s3Client;

    @Value("${application.bucket.name}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${application.bucket.public-url:}")
    private String publicUrlBase;

    public String uploadImage(MultipartFile file, UUID userId, UUID pantryItemId) throws IOException {
        String originalFilename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String key = "pantry-images/" + userId + "/" + pantryItemId + "/" + System.currentTimeMillis() + "_" + originalFilename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            if (publicUrlBase != null && !publicUrlBase.isBlank()) {
                String baseUrl = publicUrlBase.endsWith("/") ? publicUrlBase.substring(0, publicUrlBase.length() - 1) : publicUrlBase;
                return baseUrl + "/" + key;
            }

            return endpoint + "/" + bucketName + "/" + key;
        } catch (S3Exception exception) {
            throw new IOException("Failed to upload pantry image: " + exception.awsErrorDetails().errorMessage(), exception);
        }
    }
}
