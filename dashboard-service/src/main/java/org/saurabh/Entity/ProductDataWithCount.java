package org.saurabh.Entity;

import com.saurabh.userDB.entity.ProductDB;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDataWithCount {
    private ProductDB products;
    private Long productViewCount;
}
