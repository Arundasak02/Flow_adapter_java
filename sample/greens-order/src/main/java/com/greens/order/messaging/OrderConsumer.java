package com.greens.order.messaging;
import org.springframework.kafka.annotation.KafkaListener;
public class OrderConsumer { @KafkaListener(topics={"${orders.topic}"}) public void onMessage(String payload){} }