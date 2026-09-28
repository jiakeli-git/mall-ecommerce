package com.mall2.Product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductQueryParam {

    private String keyword;
    private String productSn;
    private Long categoryId;
    private Long brandId;
    private Integer publishStatus;
    private Integer verifyStatus;

    /**
     * 当前页码，必须 >= 1
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页条数，必须 >= 1
     */
    @Min(value = 1, message = "每页条数必须大于0")
    private Integer pageSize = 10;
}