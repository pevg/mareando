package com.pevg.www.concurrencia.dtos;

import lombok.Data;


import java.util.List;

@Data
public class TaskRequestDTO {
    private List<TaskDTO> shippings;

}
