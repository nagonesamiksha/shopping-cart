package com.saurabh.notification.service;
import org.apache.kafka.clients.consumer.*;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Service
public class KafkaServiceForWelcomeEmail {
    public static void consumeEmailFromTopic()
    {
        System.out.println("I am inside consumer");
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "email-consumer-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        String topic ="EmailTopic";
        consumer.subscribe(Collections.singleton(topic));
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String email = record.value();
                    System.out.println("Sending welcome mail to "+email);
                    sendEmail(email);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the consumer when done
            consumer.close();
        }

    }
    private static boolean sendEmail(String email) {
        // Email configuration properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // SMTP account credentials
        String senderEmail = "manesaurabh264@gmail.com";
        String senderPassword = "jmvtgivwjwhimybz";

        // Create a session with SMTP server
        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("manesaurabh264@gmail.com","jmvtgivwjwhimybz" );
            }
        });

        try {

            MimeMessage message = new MimeMessage(session);


            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient( Message.RecipientType.TO, new InternetAddress(email));


            message.setSubject("Welcome to the OSC");


            String emailContent = "We are thrilled to welcome you as Customer";
            message.setText(emailContent);


            Transport.send(message);

            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }

    }
}
