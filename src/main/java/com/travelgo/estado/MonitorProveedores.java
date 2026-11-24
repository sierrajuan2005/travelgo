package com.travelgo.estado;

import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.proveedor.destinos.FuenteExterna;
import com.travelgo.proveedor.destinos.ProveedorExternoException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Envuelve cada adaptador de API externa (OpenMeteoClima, XoteloProveedorHoteles, IgnavProveedorVuelos...)
 * en un proxy que mide cada llamada real: cuánto tardó y si respondió, vino vacía o falló.
 *
 * Patrón Proxy / decorador: los adaptadores (de varios integrantes) no cambian ni saben que los miden
 * (abierto/cerrado), y los servicios siguen hablando con las mismas interfaces. Solo se miden los métodos de
 * las interfaces del proveedor (no nombreFuente(), atribucion()...), y como las cachés están en los servicios,
 * cada llamada medida es una consulta real a la API.
 *
 * También hace la caída simulada del panel: si el administrador "cae" una API, el proxy falla igual que la API
 * real ({@link FuenteMonitoreada.FormaDeFallar}), así se ve en vivo cómo se defiende la app.
 */
public class MonitorProveedores implements BeanPostProcessor {

    /** Ninguna consulta real por internet tarda tan poco: si responde más rápido, salió de una caché. */
    public static final long UMBRAL_CACHE_MS = 10;

    private final ObjectProvider<RegistroLlamadas> registro;
    /** APIs que esta app usa de verdad (por ejemplo, Wikipedia u OpenTripMap para las atracciones, no las dos). */
    private final Set<String> activas = ConcurrentHashMap.newKeySet();

    public MonitorProveedores(ObjectProvider<RegistroLlamadas> registro) {
        this.registro = registro;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String nombre) {
        Optional<FuenteMonitoreada> fuente = FuenteMonitoreada.porClase(bean.getClass());
        if (fuente.isEmpty()) {
            return bean;
        }
        activas.add(fuente.get().id());
        ProxyFactory fabrica = new ProxyFactory(bean);
        // Los servicios reciben las interfaces (ProveedorClima, ProveedorHoteles...): proxy por interfaz
        fabrica.setProxyTargetClass(false);
        fabrica.addAdvice(new Medidor(fuente.get(), registro));
        return fabrica.getProxy();
    }

    /** Ids de las APIs que tienen un adaptador en esta app (las que muestra el panel). */
    public Set<String> activas() {
        return Set.copyOf(activas);
    }

    /** Mide una llamada y la registra. */
    static final class Medidor implements MethodInterceptor {

        private final FuenteMonitoreada fuente;
        private final ObjectProvider<RegistroLlamadas> registro;

        Medidor(FuenteMonitoreada fuente, ObjectProvider<RegistroLlamadas> registro) {
            this.fuente = fuente;
            this.registro = registro;
        }

        @Override
        public Object invoke(MethodInvocation invocacion) throws Throwable {
            Method metodo = invocacion.getMethod();
            RegistroLlamadas r = registro.getIfAvailable();
            if (r == null || !esConsulta(metodo)) {
                return invocacion.proceed();
            }
            if (r.estaCaida(fuente.id())) {
                r.registrar(fuente.id(), new Llamada(Instant.now(), 0, Llamada.Resultado.SIMULADA,
                        "Caída simulada desde el panel de estado."));
                return fallar(metodo, invocacion.getThis());
            }

            long inicio = System.nanoTime();
            try {
                Object respuesta = invocacion.proceed();
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                boolean vacia = esVacia(respuesta);
                if (fuente.cacheInterna() && ms < UMBRAL_CACHE_MS) {
                    r.registrar(fuente.id(), new Llamada(Instant.now(), ms, Llamada.Resultado.CACHE, null));
                    return respuesta;
                }
                r.registrar(fuente.id(), new Llamada(Instant.now(), ms,
                        vacia ? Llamada.Resultado.VACIA : Llamada.Resultado.OK,
                        vacia && fuente.forma() == FuenteMonitoreada.FormaDeFallar.RESPUESTA_VACIA
                                ? "Respondió sin resultados (esta API no informa sus errores; revisa el log)."
                                : null));
                return respuesta;
            } catch (Throwable e) {
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                r.registrar(fuente.id(), new Llamada(Instant.now(), ms, Llamada.Resultado.ERROR, e.getMessage()));
                throw e;
            }
        }

        /** Solo las consultas a la API, no los datos de atribución (nombreFuente, atribucion, enlaceFuente). */
        static boolean esConsulta(Method metodo) {
            return metodo.getDeclaringClass() != Object.class && metodo.getDeclaringClass() != FuenteExterna.class;
        }

        static boolean esVacia(Object respuesta) {
            return respuesta == null
                    || (respuesta instanceof Optional<?> o && o.isEmpty())
                    || (respuesta instanceof Collection<?> c && c.isEmpty())
                    || (respuesta instanceof Map<?, ?> m && m.isEmpty());
        }

        /** Falla como fallaría la API de verdad sin internet. */
        private Object fallar(Method metodo, Object proxy) {
            String mensaje = fuente.nombre() + " no respondió a tiempo o no hay conexión a internet (simulado).";
            return switch (fuente.forma()) {
                case EXCEPCION_DESTINOS -> {
                    String nombreFuente = proxy instanceof FuenteExterna f ? f.nombreFuente() : fuente.nombre();
                    throw new ProveedorExternoException(nombreFuente, mensaje, null);
                }
                case EXCEPCION_PROVEEDOR -> throw new ProveedorNoDisponibleException(mensaje);
                case RESPUESTA_VACIA -> vacioPara(metodo.getReturnType());
            };
        }

        static Object vacioPara(Class<?> tipo) {
            if (List.class.isAssignableFrom(tipo) || Collection.class.isAssignableFrom(tipo)) {
                return List.of();
            }
            if (Optional.class.equals(tipo)) {
                return Optional.empty();
            }
            return null;
        }
    }
}
