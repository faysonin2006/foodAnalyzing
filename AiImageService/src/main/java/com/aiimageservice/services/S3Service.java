package com.aiimageservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${application.bucket.name}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${application.bucket.public-url:}")
    private String publicUrlBase;

    public String uploadImage(MultipartFile file, String userId) throws IOException {
        String key = "food-images/" + userId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("SUCCESS: Image uploaded to bucket '{}' with key '{}'", bucketName, key);

            try {
                software.amazon.awssdk.services.s3.model.HeadObjectRequest headReq = software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.headObject(headReq);
                log.info("VERIFICATION: File '{}' exists in S3!", key);
            } catch (Exception e) {
                log.error("VERIFICATION FAILED: File '{}' was NOT found in S3 immediately after upload. Reason: {}", key, e.getMessage());
            }

            if (publicUrlBase != null && !publicUrlBase.isBlank()) {
                String baseUrl = publicUrlBase.endsWith("/") ? publicUrlBase.substring(0, publicUrlBase.length() - 1) : publicUrlBase;
                String cleanKey = key.startsWith("/") ? key.substring(1) : key;
                return baseUrl + "/" + cleanKey;
            } else {
                return endpoint + "/" + bucketName + "/" + key;
            }

        } catch (S3Exception e) {
            log.error("S3 Upload Failed: {}", e.getMessage());
            throw new IOException("Failed to upload to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }


    public byte[] downloadImageByUrl(String imageUrl) throws IOException {
        String key = extractKeyFromUrl(imageUrl);
        log.info("Downloading image. URL: '{}', Extracted Key: '{}'", imageUrl, key);

        try {
            GetObjectRequest getOb = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(getOb).readAllBytes();

        } catch (S3Exception e) {
            log.error("S3 Download Error for key '{}': {}", key, e.getMessage());
            throw new IOException("Failed to download image from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private String extractKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return "";
        if (publicUrlBase != null && !publicUrlBase.isBlank()) {
            String baseUrl = publicUrlBase.endsWith("/") ? publicUrlBase : publicUrlBase + "/";
            if (imageUrl.startsWith(baseUrl)) {
                return imageUrl.substring(baseUrl.length());
            }
        }

        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            String prefix = "/file/" + bucketName + "/";
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
            if (path.startsWith("/")) return path.substring(1);
            return path;
        } catch (Exception e) {
            log.warn("Parsing failed for URL '{}', returning original", imageUrl);
            return imageUrl;
        }
    }
}
