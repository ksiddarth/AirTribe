package com.team.tasktracker.common;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details;
}
