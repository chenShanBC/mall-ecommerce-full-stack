package com.mallfei.user.domain.service;

public interface AlipayLoginStateRepository {
    void saveState(String state, int expireSeconds);
    boolean consumeState(String state);
    void saveLoginTicket(String ticket, Long userId, int expireSeconds);
    Long consumeLoginTicket(String ticket);
}
