package com.greens.order.core;

public class OrderService {

  private final PaymentService paymentService = new PaymentService();

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
    new KafkaTemplateStub().send("${orders.topic}", id);
  }
}