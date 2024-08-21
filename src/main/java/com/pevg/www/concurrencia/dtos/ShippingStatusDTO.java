package com.pevg.www.concurrencia.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShippingStatusDTO {
    private int id;
    private String state;
}
