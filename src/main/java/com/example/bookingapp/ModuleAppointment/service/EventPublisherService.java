package com.example.bookingapp.ModuleAppointment.service;

import com.example.bookingapp.ModuleAppointment.events.AppointmentEvent;
import com.example.bookingapp.ModuleAppointment.events.NotificationEvent;
import com.example.bookingapp.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig kafkaConfig;
    
    public void publishAppointmentEvent(AppointmentEvent event) {
        try {
            log.info("Publishing appointment event: {} for appointment ID: {}", 
                    event.getEventType(), event.getAppointmentId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(kafkaConfig.getAppointmentTopicName(), 
                                  event.getAppointmentId().toString(), 
                                  event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published appointment event: {} to partition: {}", 
                            event.getEventType(), 
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish appointment event: {}", event.getEventType(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing appointment event", e);
        }
    }
    
    public void publishNotificationEvent(NotificationEvent event) {
        try {
            log.info("Publishing notification event for appointment ID: {}", event.getAppointmentId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(kafkaConfig.getNotificationTopicName(), 
                                  event.getAppointmentId().toString(), 
                                  event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published notification event to partition: {}", 
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish notification event", ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing notification event", e);
        }
    }
}
