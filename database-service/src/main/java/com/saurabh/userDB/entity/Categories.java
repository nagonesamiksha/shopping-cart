package com.saurabh.userDB.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
public class Categories {
    @Id
    private String categoryId;
    private String categoryName;
    private String imagePath;
}
