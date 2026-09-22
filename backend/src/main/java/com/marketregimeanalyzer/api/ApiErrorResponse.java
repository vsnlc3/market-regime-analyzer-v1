package com.marketregimeanalyzer.api;

public record ApiErrorResponse(
        String code,
        String message
) {
}
