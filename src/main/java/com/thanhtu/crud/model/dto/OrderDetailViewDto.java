package com.thanhtu.crud.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderDetailViewDto {
    private Integer orderId;
    private Long totalAmount;
    private String nameCustomer;
    private List<ProductOrderDetailDto> list;
}
