package com.saurabh.user.service;

import com.saurabh.grpc.cservice.*;
import com.saurabh.grpc.cservice.CacheServiceGrpc;
import com.saurabh.grpc.cservice.EraseUserRequest;
import com.saurabh.grpc.cservice.EraseUserResponse;
import com.saurabh.grpc.cservice.GetUserDetails;
import com.saurabh.grpc.cservice.GetUserDetailsRequest;
import com.saurabh.grpc.cservice.IsOtpCorrect;
import com.saurabh.grpc.cservice.ResponseOfOtp;
import com.saurabh.grpc.cservice.UserDetailResponse;
import com.saurabh.grpc.cservice.UserDetails;
import com.saurabh.grpc.cservice.UserOtp;
import com.saurabh.grpc.userdbservice.*;
import com.saurabh.grpc.userdbservice.DBServiceGrpc;
import com.saurabh.grpc.userdbservice.EmailRequest;
import com.saurabh.grpc.userdbservice.EmailResponse;
import com.saurabh.grpc.userdbservice.GetUserRequestFromUserService;
import com.saurabh.grpc.userdbservice.GetUserResponseFromUserService;
import com.saurabh.grpc.usernotificationservice.*;
import com.saurabh.grpc.usernotificationservice.EmailRequestNS;
import com.saurabh.grpc.usernotificationservice.EmailResponseNS;
import com.saurabh.grpc.usernotificationservice.NotificationServiceGrpc;
import com.saurabh.user.entity.*;
import com.saurabh.user.exception.EmailNotSentException;
import com.saurabh.user.exception.UserAlreadyThereException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;


import java.util.Properties;
import java.util.Random;

import static com.saurabh.user.entity.User1.*;

@Service
public class UserFormation {
    //private static int lastAssigned =1;

    @GrpcClient("userdbservice")
    private DBServiceGrpc.DBServiceBlockingStub stub;

    @GrpcClient("NService")
    private NotificationServiceGrpc.NotificationServiceBlockingStub stubN;

    @GrpcClient("CService")
    private CacheServiceGrpc.CacheServiceBlockingStub stubC;


    public boolean eraseUser(String id) {
        EraseUserRequest eraseUserRequest = EraseUserRequest.newBuilder().setUserId(id).build();

        EraseUserResponse eraseUserResponse = stubC.eraseUserDetailsFromCache(eraseUserRequest);
        return eraseUserResponse.getIsUserErased();
    }

    public String createUser(User1 user1) {
        System.out.println("hey i am in the user formation part");
        Boolean userExists = checkUserExists(user1.getEmail());
        System.out.println(userExists);
        if(Boolean.TRUE.equals(userExists)) {
            throw new UserAlreadyThereException();
        }
        String resultStatus = sendEmailAndOtp(user1);
        System.out.println("Returning value from createUser"+resultStatus);
      return resultStatus;
    }

    public boolean checkUserExists(String email) { //make it private if not used elsewere,pass only the email
        EmailRequest request = EmailRequest.newBuilder()
                .setEmail(email)
                .build();
        EmailResponse response = stub.checkEmail(request);
        System.out.println("Email already exists:"+response.getIsExists()); // to test the response.
        return response.getIsExists();
    }


     public String sendEmailAndOtp(User1 user1) {
//        boolean isUserThere= checkUserExists(user1);
//         if(!isUserThere) {
//             throw new UserAlreadyThereException();
//         }
         IdAndOtp idAndOtpToNs =  oTPGeneration();
         System.out.println(idAndOtpToNs.getUserId());
         EmailRequestNS requestNS = EmailRequestNS.newBuilder().setOtp(idAndOtpToNs.getOTP()).setEmail(user1.getEmail()).build();
         EmailResponseNS responseNS = null;
         responseNS = stubN.sendEmail(requestNS);

         if(!responseNS.getSuccess())
         {
             throw new EmailNotSentException("Email not sent");
         }

         SendDataToCache(user1,idAndOtpToNs.getUserId());// do this from the controller only
         sendIdAndOtpToCache(idAndOtpToNs.getUserId(),idAndOtpToNs.getOTP()); //do this from user service controller
         System.out.println(idAndOtpToNs.getUserId()); //do everything from the controller ,avoid method to method call
         return idAndOtpToNs.getUserId();
         //return true;
     }
     //cache service call

    private void SendDataToCache(User1 user1, String id) {
        UserDetails userDetails = userDetails = UserDetails.newBuilder().setUserid(id).setName(user1.getName())
                    .setEmail(user1.getEmail()).setContact(user1.getContact()).setDob(user1.getDOB()).build();

        UserDetailResponse userDetailResponse = stubC.sendUserDetails(userDetails);
    } //merge both the maps in one map

    private void sendIdAndOtpToCache(String id,String otp) {
        UserOtp userOtp = UserOtp.newBuilder().setUserid(id).setOtp(otp).build();

        ResponseOfOtp responseOfOtp = stubC.sendUserIdAndOtp(userOtp);
    }

    public static IdAndOtp oTPGeneration() {  //method names should be verbs....generatteOtp

             String digits ="123456";
             Random random = new Random();
             StringBuilder otpBuilder = new StringBuilder();

             for(int i=0;i<digits.length();i++)
             {
                 int index = random.nextInt(digits.length());
                 otpBuilder.append(digits.charAt(index));
             }


             String otp1 =otpBuilder.toString();
             String id1 = createId(otp1);
             IdAndOtp idAndOtp = new IdAndOtp();
             idAndOtp.setOTP(otp1);
             idAndOtp.setUserId(id1);
             //for sending data to cache service after the creation


             return idAndOtp;


     }

     public boolean sendUserDataToDB(UserInformation user){
         GetUserRequestFromUserService getUserRequestFromUserService= GetUserRequestFromUserService.newBuilder().setUserid(user.getUserId())
                         .setName(user.getName()).setEmail(user.getEmail()).setContact(user.getContact()).setDob(user.getDob())
                         .setPassword(user.getPassword()).build();


        GetUserResponseFromUserService getUserResponseFromUserService= stub.sendDataToDB(getUserRequestFromUserService);
        return getUserResponseFromUserService.getIsReceived();
     }

     public static  String createId(String otp) {



        String userid = "user"+otp;
        return userid;


     }

    public String validateOtp(String id1, String otp1) {
        UserOtp userOtp = UserOtp.newBuilder().setUserid(id1).setOtp(otp1).build();

        IsOtpCorrect isOtpCorrect =stubC.checkOtp(userOtp); //get the data from method and do the comparison here

        return isOtpCorrect.getCode();
    }

    public User1 getUserInfo(String id) {
        GetUserDetails getUserDetails = stubC.getUserDetailsFromCache(GetUserDetailsRequest.newBuilder().setUserid(id).build()); //dont use verbs for the class name,use it as UserDetails
       User1 user1= new User1();
        System.out.println("hey i am in get user info"+user1);
        System.out.println("status of getuserdetails:"+getUserDetails.getStatus());
       if(getUserDetails.getStatus()==true)
       {
           System.out.println("i am building the user1");
           user1.setName(getUserDetails.getName());
           user1.setEmail(getUserDetails.getEmail());
           user1.setContact(getUserDetails.getContact());
           user1.setDOB(getUserDetails.getDob());
           System.out.println("i retruning user1"+user1);
           return user1;
       }
        System.out.println("i retruning user1"+user1);
        return user1;

    }



}

/*import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.util.Properties;

public class YourClass {

    public static void sendWelcomeEmail(String email) {
        // Kafka producer configuration
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Replace with your Kafka broker address
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // Send the email to the Kafka topic
        try {
            String topic = "your_topic_name"; // Replace with the desired topic name
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, email);
            producer.send(record);
            System.out.println("Email sent to Kafka topic: " + topic);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the producer when done
            producer.close();
        }
    }

    // Your other methods and code here
}

*/




/*package com.saurabh.user.service;

import com.saurabh.user.entity.User;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class UserFormation {

    @GrpcClient("userdbservice")

    //private DBServiceGrpc.DatabaseServiceBlockingStub stub;
    private com.saurabh.grpc.userdbservice.DBServiceGrpc.DBServiceBlockingStub stub;
    public User createUser(User user) {
        Boolean userExists = checkUserExists(user.getEmail());
        if(Boolean.TRUE.equals(userExists))
            throw UserAlreadyThereException();

    }
        public boolean checkUserExists(String email)
        {
            com.saurabh.grpc.userdbservice.EmailRequest request = com.saurabh.grpc.userdbservice.EmailRequest.newBuilder()
                    .setEmail(email)
                    .build();
            com.saurabh.grpc.userdbservice.EmailResponse response = stub.checkEmail(request);
            return response.getIsExists();
        }

}*/
