package org.saurabh.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClickTracker {
    private String userId;
    private String productId;
    private String categoryId;
}
