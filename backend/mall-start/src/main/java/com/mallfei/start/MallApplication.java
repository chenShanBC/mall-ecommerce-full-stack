package com.mallfei.start;

import com.mallfei.file.config.FileStorageProperties;
import com.mallfei.pay.config.AlipaySandboxProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.mallfei.**.infrastructure.persistence.mapper")
@EnableScheduling
@EnableConfigurationProperties({FileStorageProperties.class, AlipaySandboxProperties.class})
@SpringBootApplication(scanBasePackages = "com.mallfei")
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
