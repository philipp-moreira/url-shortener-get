package com.rocketseat;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static String BUCKET_NAME = "url-shortener-bucket-philipp2";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        String pathParameters = input.get("rawPath").toString();
        String shortUrlCode = pathParameters.replace("/", "");
        String keyBucket = shortUrlCode + ".json";

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input 'shortUrlCode'is required");
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(keyBucket)
                .build();

        InputStream s3ObjectStream;
        try {
            s3ObjectStream = s3Client.getObject(getRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data from S3:" + e.getMessage(), e);
        }

        UrlData urlData;
        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing URL data:" + e.getMessage(), e);
        }

        long currentTimeInSeconds = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();

        System.out.println("currentTimeInSeconds: " + String.valueOf(currentTimeInSeconds));
        System.out.println("TimeExpiration: " + String.valueOf(urlData.getTimeExpiration()));

        if (currentTimeInSeconds > urlData.getTimeExpiration()) {
            response.put("statusCode", 410);
            response.put("body", "This URL has expired.");
        } else {
            response.put("statusCode", 302);
            Map<String, String> headers = new HashMap<>();
            headers.put("Location", urlData.getOriginalUrl());
            response.put("headers", headers);
        }

        return response;
    }
}