package org.saurabh.controller;

import com.saurabh.userDB.entity.ProductDB;
import lombok.extern.slf4j.Slf4j;
import org.saurabh.Entity.DashBoardData;
import org.saurabh.Entity.UserData;
import org.saurabh.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class DashBoardController {


    @Autowired
    ProductService productService;

    @GetMapping("/user/dashBoard")
    ResponseEntity<DashBoardData> getDataForDashBoard(UserData userData){
        log.info("UserId of the User is "+userData.getUserId());
        log.info("SessionId of the User is "+userData.getSessionId());
        //boolean isUserHasViewingHistory = productService.checkIfUserHasAnyHistory(userData.getUserId()); // we need to check this with the KTable
        boolean isUserHasViewingHistory =false; //just to check
        log.info("Is User Has Some viewing History "+isUserHasViewingHistory);
        if(!isUserHasViewingHistory){
           log.info("User has no viewing history");
           productService.getAllProductsWithCounts();
           productService.getCategories();
        }
        //Else user has some viewing history
        log.info("User has some viewing history and we have return that with appropriate modifications");
        List<ProductDB> recentlyViewed = productService.getLastViewedProducts(userData.getUserId());
        List<ProductDB> productDataWithCount = productService.getAllProductsWithCounts();
        List<ProductDB> similarProducts =productService.getSimilarProducts(recentlyViewed,productDataWithCount);
        productService.getCategories();
        return null;

    }


}
