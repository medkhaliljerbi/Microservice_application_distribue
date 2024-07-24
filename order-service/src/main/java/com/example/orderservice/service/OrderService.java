package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.event.OrderPlacedEvent;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;
    
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemDtos()
                .stream()
                .map(this::mapToDtp)
                .toList();
        order.setOrderLineItems(orderLineItems);
        List<String> skuCode= order.getOrderLineItems()
                .stream()
                .map(OrderLineItems::getSkuCode).toList();
        // call inventory service , and place order if product is in stock
        InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
                .uri("http://inventory-server/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",skuCode).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
        log.info("inventoryResponsesArray is {} with size {}",
                Arrays.toString(inventoryResponsesArray),
                inventoryResponsesArray.length);
        boolean allProductsInStock= Arrays.stream(inventoryResponsesArray)
                .allMatch(InventoryResponse::isInStock);
        log.info("allProductsInStock value is {}",allProductsInStock);
        if (allProductsInStock) {
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
        } else {
       throw new IllegalArgumentException("Product is not in stock , please try again later !");
        }
    }

    private OrderLineItems mapToDtp(OrderLineItemDto orderLineItemDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemDto.getPrice());
        orderLineItems.setQuantity(orderLineItemDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemDto.getSkuCode());
        return orderLineItems;
    }
}
