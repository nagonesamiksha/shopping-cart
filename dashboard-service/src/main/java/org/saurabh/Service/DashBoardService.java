package org.saurabh.Service;

import com.saurabh.grpc.userdbservice.DBServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.saurabh.Entity.DashBoardData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
@Slf4j
@Service
public class DashBoardService {
    @GrpcClient("userdbservice")
    private DBServiceGrpc.DBServiceBlockingStub stubDB2;

    public DashBoardData getDataForFreshUser() {


        return null;
    }

}
