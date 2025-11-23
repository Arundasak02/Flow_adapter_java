package com.greens.order.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private final PaymentService paymentService = new PaymentService();

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  public String placeOrder(String id) {
    validateCart(id);
    checkInventory(id);
    paymentService.charge(id);
    publishEvent(id);
    saveOrder(id);
    return "OK";
  }

  private void validateCart(String id) {
  }

  private void checkInventory(String id) {
  }

  private void saveOrder(String id) {
  }

  private void publishEvent(String id) {
    kafkaTemplate.send("orders.topic", id);
  }
}