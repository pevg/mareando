package com.pevg.www.concurrencia.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class TaskShippingDTO {
    private int id;
    private int shippingId;
    private Date startDate;
    private Date endDate;
}
