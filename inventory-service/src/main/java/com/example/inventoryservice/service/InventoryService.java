package com.example.inventoryservice.service;


import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        List<InventoryResponse> inventoryResponses=new ArrayList<>();
        log.info("isInStock *******************");
       List<Inventory> inventories= inventoryRepository.findBySkuCodeIn(skuCode);
        for (Inventory e: inventories) {
            log.info("is in stock {}",e.getQuantity() > 0);
        }
        if (inventories.isEmpty()){
            inventoryResponses.add(InventoryResponse.builder().isInStock(false).build());
        }else {
            inventoryResponses=  inventoryRepository.findBySkuCodeIn(skuCode).stream()
                    .map(inventory ->
                            InventoryResponse.builder()
                                    .skuCode(inventory.getSkuCode())
                                    .isInStock(inventory.getQuantity() > 0)
                                    .build()
                    ).toList();
        }
        return  inventoryResponses;
    }


}
