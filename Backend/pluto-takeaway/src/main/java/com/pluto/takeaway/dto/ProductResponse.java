package com.pluto.takeaway.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, String status) {
}
