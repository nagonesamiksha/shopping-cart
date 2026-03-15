package org.saurabh.Entity;
//
//import com.saurabh.userDB.entity.ProductDB;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDataWithCount {
    private ProductDB1 products;
    private Long productViewCount;
}
