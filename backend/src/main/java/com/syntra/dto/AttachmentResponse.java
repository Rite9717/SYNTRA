package com.syntra.dto;

public record AttachmentResponse(Long id, String originalName, String contentType, long sizeBytes) {}
