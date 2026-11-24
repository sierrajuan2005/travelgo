package com.travelgo.recordatorio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * Ejecuta la revisión de recordatorios cada cierto tiempo (por defecto cada 5 minutos, empezando 1 minuto después
 * de arrancar).
 * <p>
 * Se apaga con {@code travelgo.recordatorios.activo=false} (por ejemplo, en las pruebas). Aun encendido, no envía
 * nada si ningún canal tiene credenciales.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "travelgo.recordatorios.activo", havingValue = "true", matchIfMissing = true)
public class ProgramadorRecordatorios {

    private final RecordatorioService recordatorioService;

    public ProgramadorRecordatorios(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    @Scheduled(initialDelayString = "${travelgo.recordatorios.retraso-inicial-ms:60000}",
            fixedDelayString = "${travelgo.recordatorios.intervalo-ms:300000}")
    public void revisar() {
        recordatorioService.revisar(LocalDateTime.now());
    }
}
