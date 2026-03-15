package com.saurabh.userDB.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Products {
    @Id
    private String productId;

    private String categoryId;
    private String productName;
    private long productPrice;
    private String productDescription;
    private Long viewCount;
    private String imagePath;
}
/*

| ProductId          | varchar(10)   | NO   | PRI | NULL    |       |
        | CategoryId         | char(1)       | YES  | MUL | NULL    |       |
        | ProductName        | varchar(100)  | YES  |     | NULL    |       |
        | ProductPrice       | decimal(10,2) | YES  |     | NULL    |       |
        | ProductDescription | text          | YES  |     | NULL    |       |
        | ViewCount          | int           | YES  |     | NULL    |       |
        | ImagePath          | varchar(255)*/