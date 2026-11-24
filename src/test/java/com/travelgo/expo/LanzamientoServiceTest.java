package com.travelgo.expo;

import com.travelgo.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de las reglas de "Lanza tu viaje" con un DAO en memoria y un reloj que se mueve a mano. */
class LanzamientoServiceTest {

    private DAOEnMemoria dao;
    private RelojManual reloj;
    private LanzamientoService service;

    @BeforeEach
    void preparar() {
        dao = new DAOEnMemoria();
        reloj = new RelojManual(Instant.parse("2026-12-01T15:00:00Z"));
        service = new LanzamientoService(dao, new CatalogoDestinosSonados(), reloj);
    }

    @Test
    @DisplayName("Lanzar guarda el nombre limpio y devuelve el destino con sus coordenadas")
    void lanzarGuardaElViaje() {
        LanzamientoService.ViajeLanzado viaje = service.lanzar("  Ana  ", "paris", "cel-1");

        assertThat(viaje.lanzamiento().nombre()).isEqualTo("Ana");
        assertThat(viaje.destino().ciudad()).isEqualTo("París");
        assertThat(viaje.destino().latitud()).isEqualTo(48.857);
        assertThat(dao.filas).hasSize(1);
    }

    @Test
    @DisplayName("Un destino que no está en la lista no se puede lanzar")
    void destinoDesconocido() {
        assertThatThrownBy(() -> service.lanzar("Ana", "atlantida", "cel-1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(dao.filas).isEmpty();
    }

    @Test
    @DisplayName("Un nombre inválido no se guarda")
    void nombreInvalidoNoSeGuarda() {
        assertThatThrownBy(() -> service.lanzar("A", "paris", "cel-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dao.filas).isEmpty();
    }

    @Test
    @DisplayName("El mismo celular espera 15 segundos entre lanzamientos; otro celular no espera")
    void esperaEntreLanzamientos() {
        service.lanzar("Ana", "paris", "cel-1");

        reloj.avanzar(Duration.ofSeconds(5));
        assertThatThrownBy(() -> service.lanzar("Ana", "tokio", "cel-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("10 segundos");
        service.lanzar("Luis", "tokio", "cel-2");

        reloj.avanzar(Duration.ofSeconds(10));
        service.lanzar("Ana", "tokio", "cel-1");

        assertThat(dao.filas).hasSize(3);
    }

    @Test
    @DisplayName("Si el nombre falla, no cuenta como lanzamiento para la espera")
    void errorNoActivaLaEspera() {
        assertThatThrownBy(() -> service.lanzar("A", "paris", "cel-1"))
                .isInstanceOf(IllegalArgumentException.class);

        service.lanzar("Ana", "paris", "cel-1");
        assertThat(dao.filas).hasSize(1);
    }

    @Test
    @DisplayName("La pantalla trae todo la primera vez y después solo los nuevos, sin los ocultos")
    void pantallaTraeLosNuevosSinOcultos() {
        long primero = service.lanzar("Ana", "paris", "cel-1").lanzamiento().id();
        long segundo = service.lanzar("Luis", "tokio", "cel-2").lanzamiento().id();
        service.lanzar("Sara", "cusco", "cel-3");
        service.cambiarOculto(segundo, true);

        LanzamientoService.Pantalla inicial = service.pantalla(0);
        assertThat(inicial.total()).isEqualTo(2);
        assertThat(inicial.viajes()).extracting(v -> v.lanzamiento().nombre()).containsExactly("Ana", "Sara");

        LanzamientoService.Pantalla nuevos = service.pantalla(primero);
        assertThat(nuevos.viajes()).extracting(v -> v.lanzamiento().nombre()).containsExactly("Sara");
    }

    @Test
    @DisplayName("Ocultar un viaje que no existe avisa con RecursoNoEncontrado")
    void ocultarInexistente() {
        assertThatThrownBy(() -> service.cambiarOculto(99, true)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Los lanzamientos de un destino que ya no está en la lista no se pintan")
    void destinoRetiradoNoSePinta() {
        dao.guardar("Ana", "destino-viejo", LocalDateTime.now(), null);
        service.lanzar("Luis", "tokio", "cel-1");

        assertThat(service.pantalla(0).viajes()).extracting(v -> v.destino().id()).containsExactly("tokio");
    }

    @Test
    @DisplayName("El viaje de la página del destino trae su destino; uno oculto o que no existe no se encuentra")
    void viajeParaLaPaginaDelDestino() {
        long id = service.lanzar("Ana", "cartagena", "cel-1").lanzamiento().id();

        assertThat(service.viaje(id).destino().aeropuerto()).isEqualTo("CTG");

        service.cambiarOculto(id, true);
        assertThatThrownBy(() -> service.viaje(id)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.viaje(99)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("El celular solo se guarda con autorización y queda normalizado; también se puede borrar")
    void contactoConAutorizacion() {
        long id = service.lanzar("Ana", "paris", "cel-1").lanzamiento().id();

        assertThatThrownBy(() -> service.registrarContacto(id, "300 123 4567", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("autorización");
        assertThat(dao.buscar(id).orElseThrow().telefono()).isNull();

        assertThat(service.registrarContacto(id, "300 123 4567", true)).isEqualTo("+573001234567");
        assertThat(dao.buscar(id).orElseThrow().telefono()).isEqualTo("+573001234567");

        service.borrarContacto(id);
        assertThat(dao.buscar(id).orElseThrow().telefono()).isNull();
    }

    @Test
    @DisplayName("El embudo cuenta cuántos visitantes llegan a cada paso y calcula el porcentaje")
    void embudoDelStand() {
        long ana = service.lanzar("Ana", "cartagena", "cel-1").lanzamiento().id();
        long luis = service.lanzar("Luis", "paris", "cel-2").lanzamiento().id();
        service.lanzar("Sara", "paris", "cel-3");
        service.lanzar("Tom", "tokio", "cel-4");
        service.marcarVioDestino(ana);
        service.marcarVioDestino(luis);
        service.marcarCompartioPostal(luis);
        service.registrarContacto(ana, "3001234567", true);
        service.marcarAbrioPaquete(ana);

        LanzamientoDAO.Embudo embudo = service.embudo();

        assertThat(embudo).isEqualTo(new LanzamientoDAO.Embudo(4, 2, 1, 1, 0, 1, 0));
        assertThat(embudo.porcentaje(embudo.vieronDestino())).isEqualTo(50);
        assertThat(service.sonadoresDe("paris")).isEqualTo(2);
        assertThat(new LanzamientoDAO.Embudo(0, 0, 0, 0, 0, 0, 0).porcentaje(0)).isZero();
    }

    @Test
    @DisplayName("Quien llega por la postal de un amigo se une a su viaje; con un enlace viejo lanza normal")
    void unirseAlViajeDeUnAmigo() {
        long jaime = service.lanzar("Jaime", "cancun", "cel-1").lanzamiento().id();

        Lanzamiento ana = service.lanzar("Ana", "cancun", "cel-2", jaime).lanzamiento();
        assertThat(ana.invitadoPor()).isEqualTo(jaime);
        assertThat(ana.invitadoPorNombre()).isEqualTo("Jaime");
        assertThat(service.pantalla(0).viajes()).extracting(v -> v.lanzamiento().invitadoPorNombre())
                .containsExactly(null, "Jaime");

        service.cambiarOculto(jaime, true);
        Lanzamiento luis = service.lanzar("Luis", "cancun", "cel-3", jaime).lanzamiento();
        assertThat(luis.invitadoPor()).isNull();
        assertThat(service.lanzar("Sara", "cancun", "cel-4", 999L).lanzamiento().invitadoPor()).isNull();

        assertThat(service.embudo().llegaronPorPostal()).isEqualTo(1);
    }

    /* ---------- dobles de prueba ---------- */

    static final class DAOEnMemoria implements LanzamientoDAO {
        final List<Lanzamiento> filas = new ArrayList<>();
        final Map<Long, EnvioWhatsApp> whatsapp = new HashMap<>();

        @Override
        public long guardar(String nombre, String destinoId, LocalDateTime fecha, Long invitadoPor) {
            long id = filas.size() + 1;
            String invitador = invitadoPor == null ? null : buscar(invitadoPor).map(Lanzamiento::nombre).orElse(null);
            filas.add(new Lanzamiento(id, nombre, destinoId, fecha, false, null, invitadoPor, invitador));
            return id;
        }

        @Override
        public List<Lanzamiento> recientesVisibles(int limite) {
            List<Lanzamiento> visibles = filas.stream().filter(l -> !l.oculto()).toList();
            return visibles.subList(Math.max(0, visibles.size() - limite), visibles.size());
        }

        @Override
        public List<Lanzamiento> visiblesDesde(long desdeId, int limite) {
            return filas.stream().filter(l -> !l.oculto() && l.id() > desdeId).limit(limite).toList();
        }

        @Override
        public List<Lanzamiento> ultimos(int limite) {
            return filas.stream().sorted(Comparator.comparingLong(Lanzamiento::id).reversed()).limit(limite).toList();
        }

        @Override
        public long contarVisibles() {
            return filas.stream().filter(l -> !l.oculto()).count();
        }

        private final Set<Long> vieron = new HashSet<>();
        private final Set<Long> abrieron = new HashSet<>();
        private final Set<Long> compartieron = new HashSet<>();

        @Override
        public Optional<Lanzamiento> buscar(long id) {
            return filas.stream().filter(l -> l.id() == id).findFirst();
        }

        @Override
        public long contarVisiblesDe(String destinoId) {
            return filas.stream().filter(l -> !l.oculto() && l.destinoId().equals(destinoId)).count();
        }

        @Override
        public boolean cambiarOculto(long id, boolean oculto) {
            return reemplazar(id, l -> new Lanzamiento(l.id(), l.nombre(), l.destinoId(), l.fecha(), oculto,
                    l.telefono(), l.invitadoPor(), l.invitadoPorNombre()));
        }

        @Override
        public boolean guardarContacto(long id, String telefono, LocalDateTime fecha) {
            return reemplazar(id, l -> new Lanzamiento(l.id(), l.nombre(), l.destinoId(), l.fecha(), l.oculto(),
                    telefono, l.invitadoPor(), l.invitadoPorNombre()));
        }

        @Override
        public void marcarVioDestino(long id) {
            vieron.add(id);
        }

        @Override
        public void marcarCompartioPostal(long id) {
            compartieron.add(id);
        }

        @Override
        public void marcarAbrioPaquete(long id) {
            abrieron.add(id);
        }

        @Override
        public void guardarWhatsApp(long id, String idMensaje, String estado, LocalDateTime fecha) {
            whatsapp.put(id, new EnvioWhatsApp(idMensaje, estado, fecha));
        }

        @Override
        public void actualizarEstadoWhatsApp(long id, String estado) {
            EnvioWhatsApp e = whatsapp.get(id);
            whatsapp.put(id, new EnvioWhatsApp(e.idMensaje(), estado, e.fecha()));
        }

        @Override
        public Optional<EnvioWhatsApp> envioWhatsApp(long id) {
            return Optional.ofNullable(whatsapp.get(id));
        }

        @Override
        public Embudo embudo() {
            List<Lanzamiento> visibles = filas.stream().filter(l -> !l.oculto()).toList();
            return new Embudo(visibles.size(),
                    visibles.stream().filter(l -> vieron.contains(l.id())).count(),
                    visibles.stream().filter(l -> compartieron.contains(l.id())).count(),
                    visibles.stream().filter(l -> l.telefono() != null).count(),
                    visibles.stream().filter(l -> whatsapp.containsKey(l.id())
                            && List.of("ENTREGADO", "LEIDO").contains(whatsapp.get(l.id()).estado())).count(),
                    visibles.stream().filter(l -> abrieron.contains(l.id())).count(),
                    visibles.stream().filter(l -> l.invitadoPor() != null).count());
        }

        private boolean reemplazar(long id, UnaryOperator<Lanzamiento> cambio) {
            for (int i = 0; i < filas.size(); i++) {
                if (filas.get(i).id() == id) {
                    filas.set(i, cambio.apply(filas.get(i)));
                    return true;
                }
            }
            return false;
        }
    }

    static final class RelojManual extends Clock {
        private Instant ahora;

        RelojManual(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
