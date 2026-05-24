package com.example.bookingapp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class Mailer {
    @Autowired
    private JavaMailSender mailSender;

    // General purpose send email method (plain text)
    @Async
    public void sendEmail(String to, String subject, String body){
        sendEmail(to, subject, body, false);
    }

    // Send email with HTML support
    @Async
    public void sendEmail(String to, String subject, String body, boolean isHtml){
        try{
            if (isHtml) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true); // true indicates HTML
                
                mailSender.send(message);
                System.out.println("HTML email sent successfully to: " + to);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("Plain text email sent successfully to: " + to);
            }
        }
        catch (Exception exp) {
            System.err.println("Error sending email: " + exp.getMessage());
            exp.printStackTrace();
        }
    }

    // To send an email with attachment
    @Async
    public void sendEmail(String to, String subject, String body, String attachment){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            System.out.println("Email sent successfully with attachment to: " + to);
        } catch (Exception e) {
            System.err.println("Error while sending mail with attachment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send email to reset password
    @Async
    public void sendEmail(String to, String resetUrl){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Reset Request");
            message.setText("Click the link to reset your password: " + resetUrl);
            mailSender.send(message);

            System.out.println("Reset password email sent successfully to: " + to);
        } catch (Exception exp) {
            System.err.println("Error while sending reset password email: " + exp.getMessage());
            exp.printStackTrace();
        }
    }
}
