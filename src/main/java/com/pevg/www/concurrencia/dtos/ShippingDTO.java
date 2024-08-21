package com.pevg.www.concurrencia.dtos;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ShippingDTO {
    private int id;
    private String state;
    private Date send_date;
    private Date arrive_date;
    private int priority;
    private List<ShippingItemDTO> shipping_items;
    private List<TaskShippingDTO> task_shippings;
}
