package com.travelgo.service;

import com.travelgo.model.Actividad;
import com.travelgo.model.Gusto;
import com.travelgo.model.Viajero;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.LugarMapa;
import com.travelgo.service.PlanViajeService.Franja;
import com.travelgo.service.PlanViajeService.Momento;
import com.travelgo.service.PlanViajeService.PlanReserva;
import com.travelgo.service.PlanViajeService.TipoMomento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El plan de cada viajero sin internet: los lugares se pasan a mano (como si vinieran de Geoapify).
 * Caso del profesor: en Roma, a quien le gusta la fiesta no se le manda a la iglesia.
 */
class PlanViajeServiceTest {

    private static LugarMapa lugar(String id, String nombre, CategoriaMapa categoria) {
        return new LugarMapa(id, nombre, categoria, 41.9, 12.5, "Roma", null, null, "Geoapify");
    }

    private final List<LugarMapa> lugaresRoma = List.of(
            lugar("1", "Basílica de Santa María la Mayor", CategoriaMapa.IGLESIA),
            lugar("2", "San Juan de Letrán", CategoriaMapa.IGLESIA),
            lugar("3", "Freni e Frizioni", CategoriaMapa.BAR),
            lugar("4", "Bar del Fico", CategoriaMapa.BAR),
            lugar("5", "Museo Nacional Romano", CategoriaMapa.MUSEO),
            lugar("6", "Panteón", CategoriaMapa.SITIO_TURISTICO));

    private final Actividad bares = new Actividad("roma-bares", 1L, "Noche de bares en Trastevere", "Tres bares",
            Gusto.FIESTA, 180000, 4);

    private static List<Momento> todos(PlanReserva plan, int viajero) {
        List<Momento> momentos = new ArrayList<>();
        plan.viajeros().get(viajero).dias().forEach(d -> momentos.addAll(d.momentos()));
        return momentos;
    }

    @Test
    @DisplayName("A quien le gusta la fiesta no se le propone una iglesia; a quien le gustan las iglesias sí")
    void cadaUnoSusGustos() {
        Viajero ana = new Viajero("Ana", EnumSet.of(Gusto.FIESTA), List.of());
        Viajero luis = new Viajero("Luis", EnumSet.of(Gusto.RELIGION, Gusto.CULTURA), List.of());
        Set<Gusto> gustos = EnumSet.of(Gusto.FIESTA, Gusto.RELIGION, Gusto.CULTURA);

        PlanReserva plan = PlanViajeService.armar("Roma", 3, List.of(ana, luis),
                PlanViajeService.repartir(lugaresRoma, gustos), new ArrayList<>());

        List<Momento> deAna = todos(plan, 0);
        List<Momento> deLuis = todos(plan, 1);
        assertTrue(deAna.stream().noneMatch(m -> m.gusto() == Gusto.RELIGION));
        assertTrue(deAna.stream().anyMatch(m -> m.titulo().equals("Freni e Frizioni") && m.franja() == Franja.NOCHE));
        assertTrue(deLuis.stream().anyMatch(m -> m.gusto() == Gusto.RELIGION));
        assertTrue(deLuis.stream().noneMatch(m -> m.gusto() == Gusto.FIESTA));
    }

    @Test
    @DisplayName("La fiesta paga va de noche, el día 1 empieza con la llegada y el último termina con el regreso")
    void actividadDeNoche() {
        Viajero ana = new Viajero("Ana", EnumSet.of(Gusto.FIESTA), List.of(bares));

        PlanReserva plan = PlanViajeService.armar("Roma", 4, List.of(ana), Map.of(), new ArrayList<>());

        var dias = plan.viajeros().get(0).dias();
        assertEquals(4, dias.size());
        assertEquals(TipoMomento.LLEGADA, dias.get(0).momentos().get(0).tipo());
        assertEquals(TipoMomento.REGRESO, dias.get(3).momentos().get(1).tipo());
        Momento noche = dias.get(0).momentos().get(2);
        assertEquals(TipoMomento.ACTIVIDAD, noche.tipo());
        assertEquals(Franja.NOCHE, noche.franja());
        assertEquals("Noche de bares en Trastevere", noche.titulo());
    }

    @Test
    @DisplayName("Si dos viajeros hacen lo mismo a la misma hora, cada uno ve 'con' el otro")
    void companeros() {
        Viajero ana = new Viajero("Ana", EnumSet.of(Gusto.FIESTA), List.of(bares));
        Viajero luis = new Viajero("Luis", EnumSet.of(Gusto.FIESTA), List.of(bares));

        PlanReserva plan = PlanViajeService.armar("Roma", 2, List.of(ana, luis), Map.of(), new ArrayList<>());

        Momento deAna = plan.viajeros().get(0).dias().get(0).momentos().get(2);
        Momento deLuis = plan.viajeros().get(1).dias().get(0).momentos().get(2);
        assertEquals(List.of("Luis"), deAna.companeros());
        assertEquals(List.of("Ana"), deLuis.companeros());
    }

    @Test
    @DisplayName("Sin lugares (la API falló) el plan sale igual, con tiempo libre")
    void sinLugares() {
        Viajero luis = new Viajero("Luis", EnumSet.of(Gusto.CULTURA), List.of());

        PlanReserva plan = PlanViajeService.armar("Roma", 2, List.of(luis), Map.of(), new ArrayList<>());

        assertFalse(todos(plan, 0).isEmpty());
        assertTrue(todos(plan, 0).stream().anyMatch(m -> m.tipo() == TipoMomento.LIBRE));
    }

    @Test
    @DisplayName("Un lugar solo entra en el gusto de su categoría real")
    void repartir() {
        var porGusto = PlanViajeService.repartir(lugaresRoma, EnumSet.of(Gusto.HISTORIA, Gusto.RELIGION));
        assertEquals(List.of("Panteón"), porGusto.get(Gusto.HISTORIA).stream().map(LugarMapa::nombre).toList());
        assertEquals(2, porGusto.get(Gusto.RELIGION).size());
    }
}
