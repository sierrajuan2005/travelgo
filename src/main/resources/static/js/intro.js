/*
 * Intro de bienvenida de TravelGo (Integrante 3): "De Valledupar al mundo".
 *
 * Un globo terráqueo en 3D empieza muy cerca de Valledupar, se aleja girando y lanza rutas en arco con
 * aviones hacia destinos del mundo. Al final el globo baja y queda como horizonte, y aparece la marca.
 *
 * - El globo (proyección, continentes, arcos, aviones) está en js/globo.js, que también usa la pantalla
 *   de la ExpoSoftware. Aquí solo va la línea de tiempo de la intro.
 * - Todo se dibuja con requestAnimationFrame para mantener 60 FPS.
 * - El script en línea de fragments/intro.html decide si esta visita ve la intro (data-state="on").
 */
(function () {
    'use strict';

    const raiz = document.getElementById('tg-intro');
    if (!raiz || raiz.dataset.state !== 'on') return;
    const G = window.TravelGoGlobo;
    const g = G && G.crear(raiz.querySelector('canvas'));
    if (!g) { raiz.dataset.state = 'off'; return; }
    raiz.dataset.running = '';
    const { aVector, lerp, suave, tramo } = G;

    /* ---------- tiempos (ms) ---------- */
    const T = {
        alejarIni: 250, alejarFin: 2900,      // la cámara sale de Valledupar al espacio
        arcosIni: 1100, arcoSep: 190, arcoDur: 1500,
        horizonteIni: 3500, horizonteFin: 4700, // el globo baja y queda como horizonte
        marca: 3800,                           // aparece el logo
        duracion: 6300,                        // empieza a irse el velo
        salida: 700                            // igual que intro.css
    };

    const ORIGEN = { nombre: 'Valledupar', lat: 10.4631, lon: -73.2532 };
    const DESTINOS = [
        { nombre: 'Cartagena', lat: 10.391, lon: -75.479, izquierda: true },
        { nombre: 'Cancún', lat: 21.162, lon: -86.852 },
        { nombre: 'Cusco', lat: -13.532, lon: -71.968 },
        { nombre: 'Nueva York', lat: 40.713, lon: -74.006 },
        { nombre: 'Buenos Aires', lat: -34.604, lon: -58.382 },
        { nombre: 'Madrid', lat: 40.417, lon: -3.704 },
        { nombre: 'París', lat: 48.857, lon: 2.352 },
        { nombre: 'Tokio', lat: 35.676, lon: 139.650 }
    ];

    window.addEventListener('resize', g.redimensionar, { passive: true });

    /* ---------- cámara ---------- */
    function moverCamara(t) {
        const alejar = suave(tramo(t, T.alejarIni, T.alejarFin));
        const horizonte = suave(tramo(t, T.horizonteIni, T.horizonteFin));
        const R0 = g.M * 1.25, R1 = g.M * (g.movil ? 0.42 : 0.36);
        // Zoom en escala logarítmica: se siente como alejarse de verdad
        let R = R0 * Math.pow(R1 / R0, alejar);
        let cy = g.H / 2;
        // Horizonte: el planeta asoma desde abajo y su borde superior queda a 62 % de la altura
        const R2 = Math.max(g.W, g.H) * 0.55;
        R = lerp(R, R2, horizonte);
        cy = lerp(cy, g.H * 0.62 + R2, horizonte);

        const lon = ORIGEN.lon + 6.5 * Math.max(0, t - 400) / 1000; // el mundo gira hacia el este
        // En el horizonte la cámara baja un poco: arriba quedan Norteamérica, Groenlandia, Europa y África
        const lat = lerp(ORIGEN.lat, 16, alejar) - 36 * horizonte;
        g.mirar({ lat, lon, R, cx: g.W / 2, cy });
    }

    /* ---------- rutas ---------- */
    const vOrigen = aVector(ORIGEN.lat, ORIGEN.lon);
    const rutas = DESTINOS.map((d, i) => ({
        ...d, ...g.crearRuta(ORIGEN, d), inicio: T.arcosIni + i * T.arcoSep
    }));

    function dibujarRutas(t, apagar) {
        if (apagar <= 0.01) return;
        for (const ruta of rutas) {
            const p = tramo(t, ruta.inicio, ruta.inicio + T.arcoDur);
            if (p <= 0) continue;
            const cabeza = suave(p);
            g.dibujarEstela(ruta, cabeza, apagar, p < 1);
            if (p < 1) g.dibujarAvion(ruta, cabeza, apagar);
        }
    }

    function dibujarLugares(t, apagar) {
        // destinos: se encienden al llegar el avión
        for (const ruta of rutas) {
            const llegada = ruta.inicio + T.arcoDur;
            const encendido = tramo(t, llegada - 150, llegada + 200);
            const p = g.pin(ruta.b, '#f5b83d', 3, Math.max(0.35, encendido) * apagar);
            if (!p) continue;
            g.onda(p.x, p.y, (t - llegada) / 900, '#f5b83d');
            if (p.z > 0.25) g.etiqueta(ruta.nombre, p.x, p.y, encendido * apagar * 0.9, false, ruta.izquierda);
        }
        // Valledupar: late todo el tiempo
        if (apagar <= 0.01) return;
        const o = g.pin(vOrigen, '#ffffff', 4.5, apagar);
        if (o) {
            g.onda(o.x, o.y, (t % 1300) / 1300, '#ffffff');
            const grande = t < T.alejarIni + 1400;
            g.etiqueta(grande ? 'Valledupar, Cesar' : 'Valledupar', o.x, o.y, apagar, grande);
        }
    }

    /* ---------- chispas que dejan los aviones ---------- */
    let ultimaChispa = 0;

    function crearChispas(t) {
        if (t - ultimaChispa < (g.movil ? 90 : 45)) return;
        ultimaChispa = t;
        for (const ruta of rutas) {
            const p = tramo(t, ruta.inicio, ruta.inicio + T.arcoDur);
            if (p <= 0.05 || p >= 0.95) continue;
            const q = g.puntoRuta(ruta, suave(p));
            if (q.visible) g.soltarChispa(q.x, q.y, t);
        }
    }

    /* ---------- bucle ---------- */
    let inicio = null, anterior = 0, raf = 0, terminada = false;

    function cuadro(ahora) {
        if (inicio === null) inicio = ahora;
        const t = ahora - inicio;
        const dt = Math.min(t - anterior, 48);
        anterior = t;

        moverCamara(t);
        // Cuando el globo baja a horizonte, rutas y nombres se apagan para que el logo respire
        const apagar = 1 - suave(tramo(t, T.horizonteIni - 200, T.horizonteIni + 600));

        g.limpiar();
        g.dibujarEstrellas(t);
        g.dibujarGlobo();
        crearChispas(t);
        g.dibujarChispas(t, dt);
        dibujarRutas(t, apagar);
        dibujarLugares(t, apagar);

        if (t > T.marca && raiz.dataset.fase !== 'marca') raiz.dataset.fase = 'marca';
        if (t >= T.duracion) terminar();
        if (!terminada || raiz.dataset.state === 'leaving') raf = requestAnimationFrame(cuadro);
    }

    function detener() {
        cancelAnimationFrame(raf);
        window.removeEventListener('resize', g.redimensionar);
        window.removeEventListener('keydown', alTeclado);
    }

    function terminar() {
        if (terminada) return;
        terminada = true;
        raiz.dataset.state = 'leaving';
        setTimeout(() => {
            raiz.dataset.state = 'off';
            detener();
        }, T.salida);
    }

    function alTeclado(e) {
        if (e.key === 'Escape') terminar();
    }

    raiz.querySelector('.tg-intro-saltar').addEventListener('click', terminar);
    window.addEventListener('keydown', alTeclado);
    raf = requestAnimationFrame(cuadro);
})();
