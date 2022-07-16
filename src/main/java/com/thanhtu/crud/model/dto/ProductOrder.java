package com.thanhtu.crud.model.dto;

import lombok.Data;

@Data
public class ProductOrder {
    private Integer orderId;
    private Integer producId;
    private String productName;
    private String productImage;
    private Integer quantity;
}
