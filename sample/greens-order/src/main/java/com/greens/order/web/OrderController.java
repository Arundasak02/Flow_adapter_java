package com.greens.order.web;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @PostMapping("/{id}")
  public String placeOrder(@PathVariable String id) {
    return new com.greens.order.core.OrderService().placeOrder(id);
  }

  @GetMapping("/{id}")
  public String getOrder(@PathVariable String id) {
    return "ORDER-" + id;
  }
}