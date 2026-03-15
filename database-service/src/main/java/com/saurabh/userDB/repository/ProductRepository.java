package com.saurabh.userDB.repository;

import com.saurabh.userDB.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository <Products,String>{

}
