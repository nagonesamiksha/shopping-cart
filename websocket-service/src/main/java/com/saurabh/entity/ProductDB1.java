package com.saurabh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDB1 {
    private String productId;

    private String categoryId;
    private String productName;
    private long productPrice;
    private String productDescription;
    private Long viewCount;
    private String imagePath;
}
