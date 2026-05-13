package com.mallfei.order.application.dto;

import java.io.Serializable;

public record OrderCancelledEvent(String orderNo, String orderStatus) implements Serializable {
}
