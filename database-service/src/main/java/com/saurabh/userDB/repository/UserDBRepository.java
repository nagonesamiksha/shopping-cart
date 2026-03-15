package com.saurabh.userDB.repository;

import com.saurabh.userDB.entity.UserDB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserDBRepository extends JpaRepository<UserDB,String> {
    //UserDB findByEmail(String email);

    @Query(value = "SELECT * from userdb where email=?1",nativeQuery = true)
    Optional<UserDB> existsByEmail(String email);
//
//    @Query(value = "SELECT * from userdb where userid=?1","SELECT * from userdb where password=?2",nativeQuery = true)
//    Optional<UserDB> existsByUsername(String username,String password);

}
