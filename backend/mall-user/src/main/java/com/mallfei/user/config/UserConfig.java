package com.mallfei.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UserAlipayLoginProperties.class)
public class UserConfig {
}
