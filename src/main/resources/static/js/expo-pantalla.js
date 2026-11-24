/*
 * Pantalla del stand de la ExpoSoftware (templates/expo/pantalla.html, Integrante 3).
 *
 * Cada visitante que lanza su viaje desde el celular aparece aquí como un avión que sale de Valledupar
 * hacia su destino, con su nombre. Las rutas se quedan dibujadas: al final del día el globo está lleno.
 *
 * Torre de control (para que 30 o más lanzamientos a la vez no se vean como un desorden):
 * - Una sola pista: entre dos despegues siempre hay una separación mínima (con un poco de azar para que no
 *   parezca un metrónomo). Si hay muchos esperando, la separación se acorta para vaciar la fila más rápido.
 * - Los vuelos reales tienen prioridad y tope de aviones en el aire; el resto espera su turno en la fila.
 * - Si más de 5 personas en la fila van al mismo destino, sale UN solo avión más grande con todos
 *   ("Ana y 5 más") y el aviso dice "6 personas van a París".
 * - Tráfico de fondo: solo cuando la fila está vacía, sin nombres, más pequeño y tenue. Cuántos aviones y
 *   cada cuánto salen cambia al azar todo el tiempo (sin patrón fijo).
 *
 * Pregunta cada 2,5 s por los viajes nuevos (GET /api/expo/lanzamientos?desde=<último id>) y cada minuto
 * recarga todo (así desaparecen los que el administrador ocultó). Con ?demo simula visitantes
 * (y con ?demo&rafaga simula grupos grandes para probar la torre de control).
 */
(function () {
    'use strict';

    const app = document.getElementById('expo-pantalla');
    const G = window.TravelGoGlobo;
    const g = app && G && G.crear(app.querySelector('canvas'));
    if (!g) return;
    const { aVector, suave, tramo } = G;
    g.escalaAvion = 1.6; // la pantalla se mira desde varios metros

    const PARAMETROS = new URLSearchParams(location.search);
    const DEMO = PARAMETROS.has('demo');
    const RAFAGA = PARAMETROS.has('rafaga');
    const ORIGEN = { lat: 10.4631, lon: -73.2532 };
    const vOrigen = aVector(ORIGEN.lat, ORIGEN.lon);

    const CADA_CONSULTA = 2500, CADA_RECARGA = 60000;
    /* Torre de control */
    const SEPARACION = 650;           // ms entre despegues cuando la fila es corta
    const SEPARACION_MINIMA = 280;    // ms entre despegues con la fila muy llena
    const MAX_REALES_EN_VUELO = 8;
    const GRUPO_DESDE = 6;            // "más de 5 personas al mismo destino" = un solo avión
    const TRAFICO_MIN = 1, TRAFICO_MAX = 5;
    const MAX_NOMBRES_GRUPO = 4;      // en el aviso: "Ana, Luis, Sara, Tom y 2 más"

    const panel = app.querySelector('.expo-panel');
    const campoTotal = app.querySelector('[data-campo="total"]');
    const campoRanking = app.querySelector('[data-campo="ranking"]');
    const campoEstado = app.querySelector('[data-campo="estado"]');
    const avisos = app.querySelector('.expo-avisos');

    /* ---------- azar ---------- */
    const azar = (a, b) => a + Math.random() * (b - a);
    const alguno = lista => lista[(Math.random() * lista.length) | 0];
    // Espera "exponencial": casi siempre corta, a veces larga. Así el tráfico no tiene ritmo fijo.
    const espera = media => -Math.log(1 - Math.random()) * media;

    /* ---------- estado ---------- */
    const destinos = new Map();   // id → { id, ciudad, ruta, vector, viajes, llegada, izquierda }
    let catalogo = [];            // destinos que se pueden elegir (para el tráfico)
    let historial = [];           // viajes ya pintados (el tráfico repite sus destinos)
    let cola = [];                // viajes reales que esperan pista, en orden de llegada
    let enVuelo = [];             // { viajes: [..], destino, inicio, duracion, real }
    let ultimoId = 0, total = 0;
    let proximoDespegue = 0;
    let objetivoTrafico = 2, cambioObjetivo = 0, proximoTrafico = 0;

    function destinoDe(v) {
        let d = destinos.get(v.destinoId);
        if (!d) {
            const ruta = g.crearRuta(ORIGEN, { lat: v.latitud, lon: v.longitud });
            d = { id: v.destinoId, ciudad: v.ciudad, ruta, vector: ruta.b, viajes: 0, llegada: -Infinity,
                // Ciudades al oeste de Valledupar llevan el nombre a la izquierda del pin
                izquierda: v.longitud < ORIGEN.lon - 1 };
            destinos.set(d.id, d);
        }
        return d;
    }

    /** Pinta de una vez (sin aviones) lo que ya había: primera carga y recarga de cada minuto. */
    function cargarTodo(datos) {
        destinos.forEach(d => { d.viajes = 0; });
        historial = [];
        for (const v of datos.viajes) {
            destinoDe(v).viajes++;
            historial.push(v);
            ultimoId = Math.max(ultimoId, v.id);
        }
        cola = cola.filter(v => v.id > ultimoId);
        total = datos.total;
        actualizarPanel();
    }

    function recibirNuevos(datos) {
        for (const v of datos.viajes) {
            if (v.id <= ultimoId) continue;
            ultimoId = v.id;
            cola.push(v);
        }
    }

    /* ---------- panel: contador y ranking ---------- */
    function actualizarPanel() {
        campoTotal.textContent = total.toLocaleString('es-CO');
        const top = [...destinos.values()].filter(d => d.viajes > 0)
            .sort((a, b) => b.viajes - a.viajes || a.ciudad.localeCompare(b.ciudad)).slice(0, 5);
        campoRanking.replaceChildren(...top.map(d => {
            const li = document.createElement('li');
            const nombre = document.createElement('span');
            nombre.textContent = d.ciudad;
            const n = document.createElement('b');
            n.textContent = d.viajes;
            li.append(nombre, n);
            return li;
        }));
        if (!top.length) {
            const li = document.createElement('li');
            li.className = 'expo-ranking-vacio';
            li.textContent = '¡Sé el primero en lanzar un viaje!';
            campoRanking.append(li);
        }
    }

    function avisar(viajes, ciudad) {
        const li = document.createElement('li');
        const quien = document.createElement('b');
        if (viajes.length === 1 && viajes[0].invitadoPor) {
            // Llegó por la postal de un amigo
            const amigo = document.createElement('b');
            amigo.textContent = viajes[0].invitadoPor;
            quien.textContent = viajes[0].nombre;
            li.classList.add('expo-aviso-grupo');
            li.append(quien, ' se unió al viaje de ', amigo, ' a ' + ciudad);
        } else if (viajes.length === 1) {
            quien.textContent = viajes[0].nombre;
            li.append(quien, ' va de Valledupar a ' + ciudad);
        } else {
            quien.textContent = viajes.length + ' personas';
            const nombres = viajes.slice(0, MAX_NOMBRES_GRUPO).map(v => v.nombre).join(', ');
            const resto = viajes.length - MAX_NOMBRES_GRUPO;
            li.classList.add('expo-aviso-grupo');
            li.append(quien, ' van a ' + ciudad + ': ' + nombres + (resto > 0 ? ' y ' + resto + ' más' : ''));
        }
        avisos.prepend(li);
        while (avisos.children.length > 5) avisos.lastElementChild.remove();
        setTimeout(() => li.classList.add('expo-aviso-sale'), 6000);
        setTimeout(() => li.remove(), 6800);
    }

    /* ---------- aviones ---------- */
    function despegar(viajes, destino, t, real) {
        // Los reales vuelan a la velocidad de siempre (el celular aterriza con ellos); el tráfico varía un poco
        const duracion = G.duracionVuelo(destino.ruta) * (real ? 1 : azar(0.9, 1.4));
        enVuelo.push({ viajes, destino, inicio: t, duracion, real });
        if (real) avisar(viajes, destino.ciudad);
    }

    function aterrizar(vuelo, t) {
        vuelo.destino.llegada = t;
        if (!vuelo.real) return;
        vuelo.destino.viajes += vuelo.viajes.length;
        historial.push(...vuelo.viajes);
        if (historial.length > 500) historial.splice(0, historial.length - 500);
        total += vuelo.viajes.length;
        actualizarPanel();
    }

    /**
     * Saca de la fila el próximo vuelo real. Si más de 5 de la fila van al mismo destino que el primero,
     * salen todos juntos en un solo avión; si no, sale solo el primero.
     */
    function siguienteVueloReal() {
        const primero = cola[0];
        const mismos = cola.filter(v => v.destinoId === primero.destinoId);
        const viajes = mismos.length >= GRUPO_DESDE ? mismos : [primero];
        cola = cola.filter(v => !viajes.includes(v));
        return viajes;
    }

    function destinoDeTrafico() {
        // Prefiere los destinos que ya soñó alguien; si no hay, cualquiera del catálogo
        if (historial.length && (Math.random() < 0.7 || !catalogo.length)) return destinoDe(alguno(historial));
        if (catalogo.length) {
            const d = alguno(catalogo);
            return destinoDe({ destinoId: d.id, ciudad: d.ciudad, latitud: d.latitud, longitud: d.longitud });
        }
        return null;
    }

    function programarDespegues(t) {
        if (t < proximoDespegue) return; // una sola pista: nadie despega antes de su turno
        const reales = enVuelo.filter(v => v.real).length;

        if (cola.length && reales < MAX_REALES_EN_VUELO) {
            const viajes = siguienteVueloReal();
            despegar(viajes, destinoDe(viajes[0]), t, true);
            // Fila llena = pista más rápida (pero nunca todos al tiempo)
            const base = Math.max(SEPARACION_MINIMA, SEPARACION - 30 * cola.length);
            proximoDespegue = t + base * azar(0.85, 1.35);
            return;
        }

        // Tráfico de fondo: solo si no hay nadie esperando, y cede espacio a los reales que van en el aire
        if (cola.length) return;
        if (t > cambioObjetivo) {
            objetivoTrafico = Math.round(azar(TRAFICO_MIN, TRAFICO_MAX));
            cambioObjetivo = t + azar(5000, 15000);
        }
        const trafico = enVuelo.length - reales;
        if (t > proximoTrafico && trafico < objetivoTrafico - Math.floor(reales / 2)) {
            const destino = destinoDeTrafico();
            if (destino) despegar([], destino, t, false);
            proximoTrafico = t + SEPARACION + espera(900);
            proximoDespegue = t + SEPARACION * azar(0.9, 1.5);
        }
    }

    /* ---------- cámara: el globo se mece entre América y Europa sin perder de vista Valledupar ---------- */
    function moverCamara(t) {
        const lado = g.W > g.H && g.W > 900; // panel a la izquierda; si no, el panel va abajo
        const libreX = lado ? panel.offsetWidth : 0;
        const libreH = lado ? g.H : g.H * 0.58;
        const cx = libreX + (g.W - libreX) / 2;
        const R = Math.min((g.W - libreX) * 0.36, libreH * 0.4);
        g.mirar({
            lat: 16 + 7 * Math.sin(t / 37000),
            lon: -48 + 26 * Math.sin(t / 26000),
            R, cx, cy: libreH / 2
        });
    }

    /* ---------- dibujo ---------- */
    function dibujarRutas() {
        for (const d of destinos.values()) {
            if (d.viajes <= 0) continue;
            // Más viajes = ruta más marcada
            g.dibujarEstela(d.ruta, 1, 1, false, Math.min(1.8, 0.8 + 0.3 * Math.log2(d.viajes)));
        }
    }

    function etiquetaDe(viajes) {
        if (viajes.length === 1) return viajes[0].nombre;
        return viajes[0].nombre + ' y ' + (viajes.length - 1) + ' más';
    }

    function dibujarVuelos(t) {
        const terminados = [];
        // Primero el tráfico y encima los reales: el nombre de quien acaba de lanzar nunca queda tapado
        const orden = enVuelo.filter(v => !v.real).concat(enVuelo.filter(v => v.real));
        for (const v of orden) {
            const p = tramo(t, v.inicio, v.inicio + v.duracion);
            const cabeza = suave(p);
            const grupo = v.viajes.length > 1;
            g.dibujarEstela(v.destino.ruta, cabeza, v.real ? 1 : 0.35, true, v.real ? 1 : 0.6);
            g.dibujarAvion(v.destino.ruta, cabeza, v.real ? 1 : 0.45, grupo ? 1.5 : v.real ? 1 : 0.65);
            const q = g.puntoRuta(v.destino.ruta, cabeza);
            if (q.visible && v.real) {
                if (Math.random() < (grupo ? 0.9 : 0.6)) g.soltarChispa(q.x, q.y, t);
                // El nombre aparece cuando el avión ya se separó de Valledupar y se apaga al llegar
                const alfa = Math.min(tramo(cabeza, 0.06, 0.16), 1 - tramo(cabeza, 0.9, 1));
                g.etiqueta(etiquetaDe(v.viajes), q.x, q.y - 10 * g.ui, alfa, true);
            }
            if (p >= 1) terminados.push(v);
        }
        for (const v of terminados) aterrizar(v, t);
        if (terminados.length) enVuelo = enVuelo.filter(v => !terminados.includes(v));
    }

    function dibujarLugares(t) {
        const o = g.pin(vOrigen, '#ffffff', 5, 1);
        for (const d of destinos.values()) {
            const activo = d.viajes > 0 || enVuelo.some(v => v.real && v.destino === d);
            if (!activo) continue;
            const p = g.pin(d.vector, '#f5b83d', 3.2, d.viajes > 0 ? 1 : 0.5);
            if (!p) continue;
            g.onda(p.x, p.y, (t - d.llegada) / 1100, '#f5b83d');
            // Muy cerca de Valledupar (Cartagena, San Andrés) el nombre taparía al de Valledupar: se ve en el ranking
            const pegado = o && Math.hypot(p.x - o.x, p.y - o.y) < 60 * g.ui;
            if (p.z > 0.2 && !pegado && d.viajes > 0) {
                g.etiqueta(d.ciudad + ' · ' + d.viajes, p.x, p.y, 0.85, false, d.izquierda);
            }
        }
        if (o) {
            g.pin(vOrigen, '#ffffff', 5, 1); // encima de las ondas de los destinos
            g.onda(o.x, o.y, (t % 1300) / 1300, '#ffffff');
            g.etiqueta('Valledupar', o.x, o.y, 1, true, true);
        }
    }

    let anterior = null;
    function cuadro(ahora) {
        const dt = anterior === null ? 16 : Math.min(ahora - anterior, 48);
        anterior = ahora;
        moverCamara(ahora);
        programarDespegues(ahora);
        g.limpiar();
        g.dibujarEstrellas(ahora);
        g.dibujarGlobo();
        dibujarRutas();
        g.dibujarChispas(ahora, dt);
        dibujarVuelos(ahora);
        dibujarLugares(ahora);
        requestAnimationFrame(cuadro);
    }

    /* ---------- servidor ---------- */
    async function consultar(url) {
        const r = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    }

    function conectado(bien) {
        campoEstado.hidden = bien;
    }

    async function recargar() {
        try {
            cargarTodo(await consultar(app.dataset.api + '?desde=0'));
            conectado(true);
        } catch (e) {
            conectado(false);
        }
    }

    async function preguntar() {
        try {
            recibirNuevos(await consultar(app.dataset.api + '?desde=' + ultimoId));
            conectado(true);
        } catch (e) {
            conectado(false);
        }
        setTimeout(preguntar, CADA_CONSULTA);
    }

    function recargarCuandoSePueda() {
        // No recargar con vuelos reales en el aire o esperando: se perdería el conteo de los que aún no llegan
        if (!cola.length && !enVuelo.some(v => v.real)) recargar();
        setTimeout(recargarCuandoSePueda, CADA_RECARGA);
    }

    /* ---------- modo demo: visitantes de mentira ---------- */
    const NOMBRES = ['Valentina', 'Santiago', 'Mariana', 'Andrés', 'Camila', 'Juan Diego', 'Sofía', 'Mateo',
        'Isabella', 'Sebastián', 'Daniela', 'Alejandro', 'Gabriela', 'Eusid', 'Laura', 'Jaime', 'Rosa', 'Carlos'];
    const CATALOGO_DEMO = [
        ['cartagena', 'Cartagena', 10.391, -75.479], ['san-andres', 'San Andrés', 12.585, -81.701],
        ['cancun', 'Cancún', 21.162, -86.852], ['cusco', 'Cusco', -13.532, -71.968],
        ['nueva-york', 'Nueva York', 40.713, -74.006], ['buenos-aires', 'Buenos Aires', -34.604, -58.382],
        ['madrid', 'Madrid', 40.417, -3.704], ['paris', 'París', 48.857, 2.352],
        ['tokio', 'Tokio', 35.676, 139.650], ['rio', 'Río de Janeiro', -22.907, -43.173],
        ['roma', 'Roma', 41.903, 12.496], ['dubai', 'Dubái', 25.205, 55.271]
    ].map(([id, ciudad, latitud, longitud]) => ({ id, ciudad, latitud, longitud }));
    let idDemo = 0;

    function viajeDemo(destino) {
        const d = destino || alguno(CATALOGO_DEMO);
        return { id: ++idDemo, nombre: alguno(NOMBRES), destinoId: d.id, ciudad: d.ciudad,
            latitud: d.latitud, longitud: d.longitud };
    }

    function simular() {
        let viajes;
        if (RAFAGA) {
            // Peor caso: 30-40 lanzamientos en la misma consulta, con un destino muy popular
            const popular = alguno(CATALOGO_DEMO);
            viajes = Array.from({ length: Math.round(azar(30, 40)) },
                () => viajeDemo(Math.random() < 0.3 ? popular : null));
        } else {
            // A veces llega un grupo de amigos (2-4 seguidos), a veces uno solo
            viajes = Array.from({ length: Math.random() < 0.25 ? Math.round(azar(2, 4)) : 1 }, () => viajeDemo());
        }
        recibirNuevos({ viajes });
        setTimeout(simular, RAFAGA ? 20000 : espera(2200));
    }

    /* ---------- pantalla completa y cursor ---------- */
    function pantallaCompleta() {
        if (document.fullscreenElement) document.exitFullscreen();
        else document.documentElement.requestFullscreen().catch(() => { /* el navegador no dejó */ });
    }
    app.querySelector('[data-accion="completa"]').addEventListener('click', pantallaCompleta);
    window.addEventListener('keydown', e => { if (e.key === 'f' || e.key === 'F') pantallaCompleta(); });

    let ocultarCursor = 0;
    window.addEventListener('mousemove', () => {
        app.classList.remove('expo-sin-cursor');
        clearTimeout(ocultarCursor);
        ocultarCursor = setTimeout(() => app.classList.add('expo-sin-cursor'), 3000);
    });
    window.addEventListener('resize', g.redimensionar, { passive: true });

    /* ---------- arranque ---------- */
    actualizarPanel();
    requestAnimationFrame(cuadro);
    if (DEMO) {
        catalogo = CATALOGO_DEMO;
        const inicial = Array.from({ length: 14 }, () => viajeDemo());
        cargarTodo({ total: inicial.length, viajes: inicial });
        simular();
    } else {
        consultar(app.dataset.destinos).then(d => { catalogo = d; }).catch(() => { /* solo tráfico con historial */ });
        recargar().then(() => {
            setTimeout(preguntar, CADA_CONSULTA);
            setTimeout(recargarCuandoSePueda, CADA_RECARGA);
        });
    }
})();
