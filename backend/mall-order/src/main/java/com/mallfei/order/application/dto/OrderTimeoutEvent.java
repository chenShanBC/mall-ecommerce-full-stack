package com.mallfei.order.application.dto;

import java.io.Serializable;

public record OrderTimeoutEvent(String orderNo) implements Serializable {
}
