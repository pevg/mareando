package com.pevg.www.concurrencia.controllers;

import com.pevg.www.concurrencia.dtos.TaskRequestDTO;
import com.pevg.www.concurrencia.services.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
@AllArgsConstructor
@Tag(name = "Task", description = "Operaciones relacionadas con las tareas concurrentes")
public class TaskController {

    @Autowired
    private final TaskService taskService;

    @PostMapping("/tarea-concurrente")
    @Operation(summary = "Tarea concurrente", description = "Permite gestionar una serie de tareas concurrentes enviadas")
    public ResponseEntity<?> handleConcurrentTask(@RequestBody TaskRequestDTO request) {
        taskService.handleConcurrentTask(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Ver status", description = "Muestra el status actual de los envíos")
    public ResponseEntity<?> getStatus() {
        return taskService.getStatus();
    }

}
