package com.travelgo.expo;

import com.travelgo.proveedor.destinos.ProveedoresDestinoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trae la foto real del destino (Wikimedia) para que la postal se dibuje en el celular.
 *
 * Por qué pasa por nuestro servidor: si el canvas del navegador dibuja una imagen de otro sitio, el navegador
 * ya no deja exportar la postal como archivo (canvas "contaminado"). Servida desde TravelGo es del mismo
 * origen y se puede compartir. No hay riesgo de que pidan cualquier dirección: la URL siempre sale de
 * {@link DestinoExpoService#info} (Wikipedia), nunca del navegador.
 *
 * Son 22 destinos y fotos de tamaño mediano, así que se guardan en memoria: cada foto se descarga una vez.
 */
@Component
public class FotosPostal {

    private static final Logger LOG = LoggerFactory.getLogger(FotosPostal.class);
    /** Una foto mediana de Wikimedia pesa unos cientos de KB; más de esto no es una foto normal. */
    static final int MAXIMO_BYTES = 5 * 1024 * 1024;

    private final RestClient http;
    private final DestinoExpoService destinoExpo;
    private final Map<String, Imagen> cache = new ConcurrentHashMap<>();

    public FotosPostal(@Qualifier(ProveedoresDestinoConfig.HTTP_PROVEEDORES) RestClient.Builder http,
                       DestinoExpoService destinoExpo) {
        this.http = http.build();
        this.destinoExpo = destinoExpo;
    }

    /** Foto del destino lista para servir; vacío si no hay foto o Wikimedia no respondió. */
    public Optional<Imagen> foto(DestinoSonado destino) {
        Imagen guardada = cache.get(destino.id());
        if (guardada != null) {
            return Optional.of(guardada);
        }
        var info = destinoExpo.info(destino);
        if (info.foto() == null) {
            return Optional.empty();
        }
        try {
            // URI.create: la URL de Wikimedia ya viene codificada (%2C...); como plantilla se codificaría dos veces
            ResponseEntity<byte[]> respuesta = http.get().uri(URI.create(info.foto().datos().url()))
                    .retrieve().toEntity(byte[].class);
            byte[] datos = respuesta.getBody();
            MediaType tipo = respuesta.getHeaders().getContentType();
            if (datos == null || datos.length == 0 || datos.length > MAXIMO_BYTES
                    || tipo == null || !"image".equals(tipo.getType())) {
                return Optional.empty();
            }
            Imagen imagen = new Imagen(datos, tipo.toString());
            cache.put(destino.id(), imagen);
            return Optional.of(imagen);
        } catch (RestClientException | IllegalArgumentException e) {
            LOG.warn("No se pudo traer la foto de {} para la postal: {}", destino.ciudad(), e.getMessage());
            return Optional.empty();
        }
    }

    /** Bytes de la imagen y su tipo (image/jpeg...). */
    public record Imagen(byte[] datos, String tipo) {
    }
}
