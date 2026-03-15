package com.saurabh.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDataWithCount {
    private Categories1 category;
    private Long viewCount;
}
