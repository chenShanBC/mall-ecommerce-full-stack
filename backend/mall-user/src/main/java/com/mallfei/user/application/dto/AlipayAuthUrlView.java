package com.mallfei.user.application.dto;

public record AlipayAuthUrlView(String authUrl, String state, int expireSeconds) {
}
