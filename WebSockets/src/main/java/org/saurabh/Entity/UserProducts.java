package org.saurabh.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jmx.export.annotation.ManagedNotifications;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserProducts {
    private String recentlyViewed;
    private String cartProducts;
}

