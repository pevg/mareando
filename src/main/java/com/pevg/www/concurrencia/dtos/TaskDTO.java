package com.pevg.www.concurrencia.dtos;

import lombok.Data;

@Data
public class TaskDTO {
    private int shippingId;
    private int timeStartInSeg;
    private boolean nextState;
}
