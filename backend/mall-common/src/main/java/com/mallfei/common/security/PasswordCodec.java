package com.mallfei.common.security;

public interface PasswordCodec {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
