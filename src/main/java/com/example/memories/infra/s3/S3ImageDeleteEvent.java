package com.example.memories.infra.s3;

import java.util.List;

public record S3ImageDeleteEvent(List<String> imageKeys) {}
