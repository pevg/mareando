package com.pevg.www.concurrencia.services;

import com.pevg.www.concurrencia.dtos.*;
import com.pevg.www.concurrencia.entities.TaskLog;
import com.pevg.www.concurrencia.repositories.TaskLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.concurrent.*;

@Service
public class TaskService {


    @Autowired
    private final TaskLogRepository taskLogRepository;
    private final ThreadPoolExecutor executorService;
    private final WebClient webClient;
    @Value("${mareos.server.url}")
    private String mareosUrl;



    public TaskService(TaskLogRepository taskLogRepository, WebClient webClient) {
        this.taskLogRepository = taskLogRepository;
        this.executorService = new ThreadPoolExecutor(
                8,  //Core pool size
                8,  //Max pool size
                0L, //Time to kill inactive thread
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>() // Pending task que
        );
        this.webClient = webClient;
    }


    public void handleConcurrentTask(TaskRequestDTO request) {
        for (TaskDTO taskDTO : request.getShippings()) {
            executorService.submit(() -> processTask(taskDTO));
        }
    }

    private void processTask(TaskDTO task) {
        try {
            Thread.sleep(task.getTimeStartInSeg() * 1000L);
            int shippingId = task.getShippingId();
            if(!isTaskInProgress(shippingId)){
                int task_id = markTaskInProgress(shippingId);
                String currentStatus = getShippingStatus(shippingId, task_id);
                String transition = task.isNextState() ? getShippingNextState(currentStatus) : "cancelado";
                if(currentStatus.equals(transition)){
                    finishTask(task_id);
                    logTask(shippingId, "FAILED", "Estado transición igual a estado actual");
                }else{
                    makeShippingTransition(task.getShippingId(), transition, task_id);
                    finishTask(task_id);
                    logTask(shippingId, "SUCCESS", "Tarea completada con éxito");
                }
            } else {
                logTask(shippingId, "FAILED", "Tarea para el mismo envío en curso");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logTask(task.getShippingId(), "FAILED", "Error al procesar la tarea");
        } catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    private boolean isTaskInProgress(int shippingId) {
        String url = mareosUrl + "/tasks/" + shippingId;
        try {
            TaskShippingDTO taskShippingDTO = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(TaskShippingDTO.class)
                    .block();

            return taskShippingDTO != null && taskShippingDTO.getEndDate() == null;

        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Error HTTP, al consultar tareas activas para el envío: " + e.getStatusCode(), e);
        }
    }

    private String getShippingStatus(int shippingId, int task_id) {
        String url = mareosUrl + "/shippings/" + shippingId;
        try {
            ShippingDTO response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ShippingDTO.class)
                    .block();
            return response != null ? response.getState() : null;
        } catch (WebClientResponseException e) {
            finishTask(task_id);
            throw new IllegalStateException("Error HTTP, al consultar estado del envío: " + e.getStatusCode(), e);
        }
    }

    private int markTaskInProgress(int shippingId) {
        String url = mareosUrl + "/tasks";

        try {
            TaskShippingDTO response = webClient.post()
                    .uri(url)
                    .bodyValue((Integer) shippingId)
                    .retrieve()
                    .bodyToMono(TaskShippingDTO.class)
                    .block();
            if (response == null) {
                throw new IllegalStateException("No se pudo iniciar la tarea de envío para shippingId: " + shippingId);
            }
            return response.getId();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Error HTTP, al iniciar tarea de envío: " + e.getStatusCode(), e);
        }
    }

    private TaskShippingDTO finishTask(int taskId) {
        String url = mareosUrl + "/tasks/" + taskId;
        try {
            TaskShippingDTO response = webClient.patch()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(TaskShippingDTO.class)
                    .block();
            if (response == null) {
                throw new IllegalStateException("No existe una tarea para finalizar con Id: " + taskId);
            }
            return response;
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Error HTTP, al terminar tarea de envío: " + e.getStatusCode(), e);
        }
    }

    private String getShippingNextState(String currentState) {
        return switch (currentState) {
            case "inicial" -> "entregado_al_correo";
            case "entregado_al_correo" -> "en_camino";
            case "en_camino" -> "entregado";
            default -> "otro";
        };
    }

    private void makeShippingTransition(int shippingId, String transition, int task_id) throws InterruptedException {
        switch (transition) {
            case "entregado_al_correo":
                System.out.println("Transicionando a enviar_al_correo");
                Thread.sleep(1000);
                break;
            case "en_camino":
                System.out.println("Transicionando a en_camino");
                Thread.sleep(3000);
                break;
            case "entregado":
                System.out.println("Transicionando a entregado");
                Thread.sleep(5000);
                break;
            case "cancelado":
                System.out.println("Transicionando a cancelado");
                Thread.sleep(3000);
                break;
        }
        String url = mareosUrl + "/shippings/" + shippingId;
        if(!transition.equals("otro")){
            try {
                webClient.patch()
                        .uri(url)
                        .bodyValue(new TransitionRequestDTO(transition))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } catch (WebClientResponseException e) {
                finishTask(task_id);
                logTask(shippingId, "FAILED", "Error al realizar la transición");
                throw new IllegalStateException("HTTP error during transition: " + e.getStatusCode(), e);
            }
        }else{
            finishTask(task_id);
            logTask(shippingId, "FAILED", "Transición incorrecta");
            throw new IllegalStateException("Transición Incorrecta");
        }
    }

    public void logTask(int shippingId, String status, String message) {
        TaskLog failledTask = new TaskLog();
        failledTask.setMessage(message);
        failledTask.setStatus(status);
        failledTask.setShippingId(shippingId);
        taskLogRepository.save(failledTask);
    }

    public ResponseEntity<List<ShippingStatusDTO>> getStatus() {
        String url = mareosUrl + "/shippings/status";
        try {
            List<ShippingStatusDTO> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ShippingStatusDTO>>() {})
                    .block();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Error HTTP, al consultar status de envíos: " + e.getStatusCode(), e);
        }
    }

}
