# Mareando - Sistema de Gestión de Tareas Concurrentes para Envíos

Este proyecto es un sistema que gestiona tareas concurrentes para el servicio de envíos de "mareo-envíos". La aplicación se encarga de manejar la ejecución de tareas de manera coordinada y evitar conflictos en la transición de estados de los envíos, comunicándose con la API de "mareo-envíos" a través de `WebClient`.

## Tabla de Contenidos

- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Arquitectura del Proyecto](#arquitectura-del-proyecto)
- [Configuración del Entorno](#configuración-del-entorno)
- [Endpoints Principales](#endpoints-principales)
- [Ejecución del Proyecto](#ejecución-del-proyecto)
- [Consideraciones Adicionales](#consideraciones-adicionales)

## Tecnologías Utilizadas

- **Java 17**: Lenguaje de programación.
- **Spring Boot**: Framework para el desarrollo de aplicaciones basadas en Java.
- **Spring WebFlux**: Para el manejo de llamadas no bloqueantes y reactivas a través de `WebClient`.
- **PostgreSQL**: Base de datos relacional.
- **Docker y Docker Compose**: Para la contenedorización y orquestación de servicios.
- **Tomcat**: Servidor de aplicaciones para desplegar el proyecto empaquetado como un archivo WAR.

## Arquitectura del Proyecto

El proyecto sigue una arquitectura basada en capas y está diseñado para interactuar con el servicio externo "mareo-envíos":

1. **Controladores**: Proveen los endpoints REST para gestionar tareas concurrentes y consultar estados.
2. **Servicios**: Contienen la lógica de negocio para manejar la concurrencia y la comunicación con "mareo-envíos".
3. **DTOs (Data Transfer Objects)**: Objetos utilizados para transferir datos entre las capas del sistema y con el servicio "mareo-envíos".
4. **Entidades**: Representan la persistencia de logs de tareas en la base de datos.
5. **Repositorios**: Interfaces para interactuar con la base de datos usando Spring Data JPA.

## Configuración del Entorno

### Requisitos Previos

- **Java 17**
- **Maven**
- **Docker y Docker Compose**

### Variables de Entorno

El sistema requiere las siguientes variables de entorno, que se configuran en el archivo `docker-compose.yml`:

- `SPRING_DATASOURCE_URL`: URL de la base de datos PostgreSQL compartida con "mareo-envíos".
- `SPRING_DATASOURCE_USERNAME`: Usuario de la base de datos.
- `SPRING_DATASOURCE_PASSWORD`: Contraseña de la base de datos.
- `MAREOS_SERVER_URL`: URL base del servicio "mareo-envíos" al que se conecta "mareando".

Ejemplo de configuración:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://<db-host>:5432/mareando
  SPRING_DATASOURCE_USERNAME: admin
  SPRING_DATASOURCE_PASSWORD: admin
  MAREOS_SERVER_URL: http://<mareos-host>:8080/api/v1
```

## Endpoints Principales

### 1. **TaskController**

#### **POST /tarea-concurrente**

- **Descripción**: Este endpoint recibe una lista de tareas concurrentes que deben ejecutarse de acuerdo con un cronograma específico.
- **Cuerpo de la solicitud (JSON)**:

```json
{
  "shippings": [
    {
      "shippingId": 1,
      "timeStartInSeg": 120,
      "nextState": true
    },
    {
      "shippingId": 2,
      "timeStartInSeg": 180,
      "nextState": false
    },
    {
      "shippingId": 3,
      "timeStartInSeg": 240,
      "nextState": true
    }
  ]
}
```

### 2. **StatusController**

#### **GET /status**

- **Descripción**: Este endpoint consulta el estado actual de todos los envíos..
- **Cuerpo de la resouesta (JSON)**:

```json
  {
    [
        {
            "id": 1,
            "state": "en_camino"
        },
        {
            "id": 2,
            "state": "entregado_al_correo"
        }
    ]
  }
```

## Ejecución del Proyecto

### Opción 1: Ejecución con Docker

Esta es la forma recomendada para ejecutar el proyecto en un entorno controlado, asegurando que todas las dependencias y configuraciones sean consistentes.

1. **Clonar el repositorio**:
   ```sh
   git clone git@github.com:pevg/mareando.git
   cd mareando
   ```
2. **Configurar la red compartida (si no existe)**:
   ```sh
   docker network create app-network
   cd mareando
   ```
3. **Compilar y construir la imagen de Docker**:
   - Primero se debe construir la aplicación de mareos, para que implemente la base de datos y la red
   ```sh
   docker-compose up --build
   ```
4. **Verificar que la aplicación está corriendo**:
   - La aplicación estará disponible en http://localhost:8082.
   - Puedes acceder a la documentación de la API en http://localhost:8082/swagger-ui.html.

### Opción 2: Ejecución Local

1. **Clonar el repositorio**:
   ```sh
   git clone git@github.com:pevg/mareando.git
   cd mareando
   ```
2. **Configurar base de datos**:

   - Asegúrate de que PostgreSQL esté corriendo localmente y que se ha creado una base de datos llamada mareando.
   - Actualiza las configuraciones en application.properties para apuntar a tu base de datos local:

   ```sh
    spring.datasource.url=jdbc:postgresql://localhost:5432/mareando
    spring.datasource.username=tu_usuario
    spring.datasource.password=tu_contraseña
    mareos.server.url=http://localhost:8081/api/v1  # URL de 'mareo-envíos'
   ```

3. **Correr Proyecto**:

   - Usa tu IDE de preferencia para compilar y correr el proyecto

4. **Verificar que la aplicación está corriendo**:
   - La aplicación estará disponible en http://localhost:8082/api/v1.
   - Puedes acceder a la documentación de la API en http://localhost:8082/api/v1/swagger-ui.html.

## Consideraciones Adicionales

### 1. **Persistencia de Tareas**

- Todas las tareas ejecutadas se registran en la base de datos, lo que permite llevar un historial de las operaciones realizadas.

### 2. **Manejo de Errores**

- La aplicación maneja errores comunes como transiciones de estado inválidas o tareas concurrentes conflictivas. Se recomienda extender esta funcionalidad para cubrir otros escenarios posibles, como fallos en la red, errores de base de datos, o tiempo de espera excedido en la comunicación con `mareo-envíos`.

### 3. **Escalabilidad**

- El diseño actual está orientado a manejar un volumen moderado de tareas concurrentes. Si se espera un aumento significativo en la carga, se sugiere:
  - Escalar horizontalmente la aplicación `mareando` utilizando múltiples instancias.
  - Implementar un sistema de colas (por ejemplo, RabbitMQ, Kafka) para manejar las tareas concurrentes y distribuir la carga entre las instancias.
  - Considerar la optimización de la base de datos y la aplicación para manejar grandes volúmenes de datos de manera eficiente. Herramientas como Prometheus y Grafana pueden ser útiles para este propósito.

### 4. **Documentación**

- La API está documentada utilizando Swagger. Puedes acceder a la documentación interactiva en `http://localhost:8082/api/v1/swagger-ui.html` una vez que la aplicación esté en funcionamiento.
- Mantén la documentación actualizada con cualquier cambio que se realice en la API para facilitar la comprensión y el uso del servicio por parte de otros desarrolladores.

### 5. **Configuración de Red**

- Asegúrate de que `mareando` y `mareo-envíos` están en la misma red Docker para permitir la comunicación entre ambos servicios.
- Si se necesita comunicar estas aplicaciones fuera de Docker, se deben exponer los puertos adecuados y configurar correctamente las variables de entorno para apuntar a las direcciones IP o nombres de host correctos.
