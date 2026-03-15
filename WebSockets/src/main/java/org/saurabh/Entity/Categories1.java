package org.saurabh.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categories1 {
    private String categoryId;
    private String categoryName;
    private String imagePath;
}
