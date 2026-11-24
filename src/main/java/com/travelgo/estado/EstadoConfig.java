package com.travelgo.estado;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el {@link MonitorProveedores}. El método es static (regla de Spring para los BeanPostProcessor:
 * se crean antes que los demás beans) y recibe el registro con ObjectProvider para no adelantar su creación.
 */
@Configuration
public class EstadoConfig {

    @Bean
    public static MonitorProveedores monitorProveedores(ObjectProvider<RegistroLlamadas> registro) {
        return new MonitorProveedores(registro);
    }
}
