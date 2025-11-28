package com.greens.order.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

  @KafkaListener(topics = {"orders.v1"})
  public void onMessage(String payload) {
  }
}