/*
 * Intro de bienvenida de TravelGo (Integrante 3): "De Valledupar al mundo".
 *
 * Un globo terráqueo en 3D (proyección ortográfica dibujada en <canvas>) empieza muy cerca de Valledupar,
 * se aleja girando y lanza rutas en arco con aviones hacia destinos del mundo. Al final el globo baja
 * y queda como horizonte, y aparece la marca.
 *
 * - Los continentes son una máscara de puntos cada 2° sacada de Natural Earth (land 110m, dominio público),
 *   guardada aquí en base64: la intro no depende de internet.
 * - Todo se dibuja con requestAnimationFrame y matrices precalculadas para mantener 60 FPS.
 * - El script en línea de fragments/intro.html decide si esta visita ve la intro (data-state="on").
 */
(function () {
    'use strict';

    const raiz = document.getElementById('tg-intro');
    if (!raiz || raiz.dataset.state !== 'on') return;
    const canvas = raiz.querySelector('canvas');
    const ctx = canvas && canvas.getContext('2d');
    if (!ctx) { raiz.dataset.state = 'off'; return; }
    raiz.dataset.running = '';

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

    /* ---------- máscara de tierra: 180 x 90 celdas de 2° (bit = tierra) ---------- */
    const MASCARA = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOAfAP8DAAAAAAAAAAAAAAAAAAAAAADo//z//wcAAAAAAAACAAAAAAAAAAAAhvvw//8PAPABAAAAwAMAAAAAAAAAwADkw////wEABAAAAABgAAAAAAAAAADAUT8A/v8PAAAAAAwA/j8AuAEAAAAAcBHtDcD/fwAAAAAwAPz/fwMAABCAAQD5w/wD8P8FAAAMAIL7//////9/AP//////////////B/D/SAAAAAAAQID//////////////x8AKAAEAAAAAAAA8///////////////YCgAAAAAAAAAAADw/////wEs4AEAAHz+////////////gL////8HeAAcAADg5///////////9ADgAf7/f4AnAAAAAH78////////H0QAAAiA//8f8AcAAIBB4////////38ADwAQAOD//5//AQAAHAT/////////A3AAAAAA/v//+T8AAGDz//////////8DAQAAAMD/////AwAAsP//////////LwAAAAAA6P///2IAAAD+//////////8CAAAAAAD///8/CAAA4P//////////JwAAAAAA8P///wYAAAD+/unz/////z8AAAAAAAD///8HAAAA/pgPPP//////MQAAAAAA8P//PwAAAMBD9v7n/////wcBAAAAAAD///8AAAAAPkD7f/7///8hEAAAAAAA4P//DwAAAIDhAv/n////f8YAAAAAAAD8//8AAAAA+AdE//////8jDwAAAAAAgP//AwAAAMD/APD/////PxgAAAAAAADw/x8AAAAA/n/v//////8HAAAAAAAAAPwDAgAAAOD////7////fwAAAAAAAACgHyAAAACA//9/f/7///8DAAAAAAAAAPQBAAAAAPj//+cv+P//PwAAAAAAAAAAHjAAAADA/////g/+//8EAAAAAAAAAOBhCAAAAP7//99/4D//AAAAAAAAAAAAPAMEAADA////+Qf84BcAAAAAAAAAAAA/AAAAAPz//58fgAf+QAAAAAAAAAAAAA8AAADg////ewA4gA8EAAAAAAAAAADAAAAAAPz//38BgAP4QQAAAAAAAAAAAAgPAADA////zwAwgAwQAAAAAAAAAAAA9Q8AAPj///8HAAVIAAAAAAAAAAAAAID/AQAA////fwBAAAAQAAAAAAAAAAAA+P8AAGDh//8DAAA0GAAAAAAAAAAAAID/HwAAAPj/HwAAgMIBAAAAAAAAAAAA/P8BAACA//8AAAAYXgAAAAAAAAAAAMD/fwAAAPz/BwAAAOOBAQAAAAAAAAAA/P8/AACA/z8AAABgbtQBAAAAAAAAAOD//w8AAPD/AwAAAAQIeAAAAAAAAAAA/P//AQAA/z8AAACAA4APAQAAAAAAAID//w8AAPD/AwAAAAARsEAAAAAAAAAA+P9/AAAA/j8AAAAAAAAAAAAAAAAAAAD//wcAAPD/QwAAAACAIwAAAAAAAAAA8P9/AAAA/z8EAAAAAD8GIAAAAAAAAAD8/wMAAPD/cQAAAAD4ZwAAAAAAAAAAgP8/AAAA/w8HAAAAgP8HAAAAAAAAAAD4/wMAAOD/MAAAAAD//wEBAAAAAAAAgP8PAAAA/g8DAAAA+P8fAAAAAAAAAAD4PwAAAOB/EAAAAID//wMAAAAAAAAAgP8DAAAA/AMAAAAA+P9/AAAAAAAAAAD8HwAAAMA/AAAAAID//wcAAAAAAAAAwP8BAAAA+AEAAAAA8P9/AAAAAAAAAAD8DwAAAIAPAAAAAAAP/gMAAAAAAAAAwB8AAAAAAAAAAAAAEIAfAAEAAAAAAAD+AwAAAAAAAAAAAAAA8AEgAAAAAAAA4AcAAAAAAAAAAAAAAAAAAAYAAAAAAABeAAAAAAAAAAAAAAAAwAAwAAAAAAAAwAMAAAAAAAAAAAAAAAAIgAEAAAAAAAAeAAAAAAAAAAAAAAAAAAAMAAAAAAAA4AEAAAAAAAAAAAAAAAAAAAAAAAAAAAAPAAAAAAAAAAABAAAAAAAAAAAAAAAAcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOAAAAAAAAAAAAAAAAAAAAAAAAAAAAwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAAAAB4AEDwn/8HAAAAAAAAAAAwAAAAAACA/P/h/////w8AAAAAAAAAwAcAAAD4////z///////PwAAAAAAHALwAACA//////////////8HAADw/y///wMAAP7/////////////HwAA+P///38AAID///////////////8AAPL/////BwAO////////////////DwAA8P////8HEPD//////////////z8AAOD/////////////////////////HwDgPwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';
    const COLS = 180, FILAS = 90, RES = 2;

    const RAD = Math.PI / 180;
    const aVector = (lat, lon) => {
        const f = lat * RAD, l = lon * RAD;
        return [Math.cos(f) * Math.cos(l), Math.cos(f) * Math.sin(l), Math.sin(f)];
    };

    const tierra = (function () {
        const bytes = atob(MASCARA);
        const puntos = [];
        for (let k = 0; k < COLS * FILAS; k++) {
            if (bytes.charCodeAt(k >> 3) & (1 << (k & 7))) {
                const j = Math.floor(k / COLS), i = k % COLS;
                puntos.push(...aVector(90 - RES / 2 - j * RES, -180 + RES / 2 + i * RES));
            }
        }
        return new Float32Array(puntos);
    })();

    /* ---------- lienzo ---------- */
    let W = 0, H = 0, M = 0, ui = 1, movil = false;

    function redimensionar() {
        const dpr = Math.min(window.devicePixelRatio || 1, 1.5);
        W = window.innerWidth;
        H = window.innerHeight;
        M = Math.min(W, H);
        canvas.width = Math.round(W * dpr);
        canvas.height = Math.round(H * dpr);
        canvas.style.width = W + 'px';
        canvas.style.height = H + 'px';
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ui = Math.max(0.7, M / 820);
        movil = W < 640;
    }
    window.addEventListener('resize', redimensionar, { passive: true });
    redimensionar();

    const limitar = (v, a, b) => Math.max(a, Math.min(b, v));
    const lerp = (a, b, t) => a + (b - a) * t;
    const suave = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
    const salida = t => 1 - Math.pow(1 - t, 3);
    const tramo = (t, a, b) => limitar((t - a) / (b - a), 0, 1);

    /* ---------- cámara ---------- */
    const cam = { cosL: 1, sinL: 0, cosP: 1, sinP: 0, R: 1, cx: 0, cy: 0 };

    function moverCamara(t) {
        const alejar = suave(tramo(t, T.alejarIni, T.alejarFin));
        const horizonte = suave(tramo(t, T.horizonteIni, T.horizonteFin));
        const R0 = M * 1.25, R1 = M * (movil ? 0.42 : 0.36);
        // Zoom en escala logarítmica: se siente como alejarse de verdad
        let R = R0 * Math.pow(R1 / R0, alejar);
        let cy = H / 2;
        // Horizonte: el planeta asoma desde abajo y su borde superior queda a 62 % de la altura
        const R2 = Math.max(W, H) * 0.55;
        R = lerp(R, R2, horizonte);
        cy = lerp(cy, H * 0.62 + R2, horizonte);

        const lon0 = ORIGEN.lon + 6.5 * Math.max(0, t - 400) / 1000; // el mundo gira hacia el este
        // En el horizonte la cámara baja un poco: arriba quedan Norteamérica, Groenlandia, Europa y África
        const lat0 = lerp(ORIGEN.lat, 16, alejar) - 36 * horizonte;
        cam.cosL = Math.cos(lon0 * RAD); cam.sinL = Math.sin(lon0 * RAD);
        cam.cosP = Math.cos(lat0 * RAD); cam.sinP = Math.sin(lat0 * RAD);
        cam.R = R; cam.cx = W / 2; cam.cy = cy;
    }

    // Proyección ortográfica. h > 1 levanta el punto sobre la superficie (rutas en arco).
    const P = { x: 0, y: 0, z: 0, visible: false };
    function proyectar(X, Y, Z, h) {
        const X1 = X * cam.cosL + Y * cam.sinL;
        const Y1 = -X * cam.sinL + Y * cam.cosL;
        const xs = Y1;
        const ys = -X1 * cam.sinP + Z * cam.cosP;
        const zs = X1 * cam.cosP + Z * cam.sinP;
        P.x = cam.cx + cam.R * h * xs;
        P.y = cam.cy - cam.R * h * ys;
        P.z = zs;
        // Detrás del globo solo se ve lo que sobresale de su silueta
        P.visible = zs > 0 || (xs * xs + ys * ys) * h * h > 1;
        return P;
    }

    /* ---------- estrellas con un poco de paralaje ---------- */
    const estrellas = Array.from({ length: 140 }, () => ({
        x: Math.random(), y: Math.random(), r: Math.random() * 1.2 + 0.3, fase: Math.random() * 6.3
    }));

    function dibujarEstrellas(t) {
        const desplazar = (t / 1000) * 9;
        ctx.fillStyle = '#ffffff';
        for (const e of estrellas) {
            ctx.globalAlpha = 0.2 + 0.4 * (0.5 + 0.5 * Math.sin(t / 650 + e.fase));
            const x = ((e.x * W - desplazar * e.r) % W + W) % W;
            ctx.fillRect(x, e.y * H, e.r, e.r);
        }
        ctx.globalAlpha = 1;
    }

    /* ---------- globo: atmósfera, océano, meridianos y tierra ---------- */
    function dibujarGlobo() {
        const { R, cx, cy } = cam;

        // atmósfera
        const atm = ctx.createRadialGradient(cx, cy, R * 0.92, cx, cy, R * 1.22);
        atm.addColorStop(0, 'rgba(128, 81, 255, 0.55)');
        atm.addColorStop(0.35, 'rgba(84, 51, 235, 0.22)');
        atm.addColorStop(1, 'rgba(84, 51, 235, 0)');
        ctx.fillStyle = atm;
        ctx.beginPath();
        ctx.arc(cx, cy, R * 1.22, 0, Math.PI * 2);
        ctx.fill();

        // océano con luz desde arriba a la izquierda
        const mar = ctx.createRadialGradient(cx - R * 0.38, cy - R * 0.42, R * 0.05, cx, cy, R);
        mar.addColorStop(0, '#3b2c9e');
        mar.addColorStop(0.55, '#1a1150');
        mar.addColorStop(1, '#07051a');
        ctx.fillStyle = mar;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.fill();

        // meridianos y paralelos
        ctx.strokeStyle = 'rgba(185, 168, 255, 0.10)';
        ctx.lineWidth = 1;
        for (let lon = -180; lon < 180; lon += 30) linea(lat => aVector(lat, lon), -90, 90);
        for (let lat = -60; lat <= 60; lat += 30) linea(lon => aVector(lat, lon), -180, 180);

        // tierra: un punto por celda, iluminado según la cara que mira a la luz
        const lado = limitar(R * 0.0105, 1.1, 6.5);
        const paso = movil ? 2 : 1;
        ctx.fillStyle = '#cfc4ff';
        for (let k = 0; k < tierra.length; k += 3 * paso) {
            const p = proyectar(tierra[k], tierra[k + 1], tierra[k + 2], 1);
            if (p.z <= 0.02 || p.x < -10 || p.x > W + 10 || p.y < -10 || p.y > H + 10) continue;
            const xs = (p.x - cx) / R, ys = (cy - p.y) / R;
            const luz = limitar(-0.45 * xs + 0.55 * ys + 0.7 * p.z, 0, 1);
            ctx.globalAlpha = 0.18 + 0.82 * luz;
            const s = lado * (0.55 + 0.45 * p.z);
            ctx.fillRect(p.x - s / 2, p.y - s / 2, s, s);
        }
        ctx.globalAlpha = 1;

        // borde iluminado
        ctx.strokeStyle = 'rgba(185, 168, 255, 0.35)';
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.stroke();
    }

    function linea(vectorDe, desde, hasta) {
        ctx.beginPath();
        let abierta = false;
        for (let v = desde; v <= hasta; v += 6) {
            const [X, Y, Z] = vectorDe(v);
            const p = proyectar(X, Y, Z, 1);
            if (p.z > 0) {
                if (abierta) ctx.lineTo(p.x, p.y); else ctx.moveTo(p.x, p.y);
                abierta = true;
            } else {
                abierta = false;
            }
        }
        ctx.stroke();
    }

    /* ---------- rutas: arcos de círculo máximo levantados sobre el globo ---------- */
    const MUESTRAS = 64;
    const vOrigen = aVector(ORIGEN.lat, ORIGEN.lon);
    const rutas = DESTINOS.map((d, i) => {
        const b = aVector(d.lat, d.lon);
        const omega = Math.acos(limitar(vOrigen[0] * b[0] + vOrigen[1] * b[1] + vOrigen[2] * b[2], -1, 1));
        const alto = 0.05 + 0.3 * (omega / Math.PI);
        const pts = [];
        for (let s = 0; s <= MUESTRAS; s++) {
            const u = s / MUESTRAS;
            const k1 = Math.sin((1 - u) * omega) / Math.sin(omega);
            const k2 = Math.sin(u * omega) / Math.sin(omega);
            pts.push({
                X: k1 * vOrigen[0] + k2 * b[0], Y: k1 * vOrigen[1] + k2 * b[1], Z: k1 * vOrigen[2] + k2 * b[2],
                h: 1 + alto * Math.sin(Math.PI * u)
            });
        }
        return { ...d, b, pts, inicio: T.arcosIni + i * T.arcoSep };
    });

    function puntoRuta(ruta, u) {
        const f = u * MUESTRAS, i = Math.min(MUESTRAS - 1, Math.floor(f)), r = f - i;
        const a = ruta.pts[i], b = ruta.pts[i + 1];
        return proyectar(lerp(a.X, b.X, r), lerp(a.Y, b.Y, r), lerp(a.Z, b.Z, r), lerp(a.h, b.h, r));
    }

    function dibujarRutas(t, apagar) {
        if (apagar <= 0.01) return;
        ctx.save();
        ctx.lineCap = 'round';
        for (const ruta of rutas) {
            const p = tramo(t, ruta.inicio, ruta.inicio + T.arcoDur);
            if (p <= 0) continue;
            const cabeza = suave(p);
            const n = Math.max(2, Math.round(MUESTRAS * cabeza));
            let prev = null;
            for (let s = 0; s <= n; s++) {
                const u = (s / n) * cabeza;
                const q = puntoRuta(ruta, u);
                const actual = { x: q.x, y: q.y, visible: q.visible };
                if (prev && prev.visible && actual.visible) {
                    // la estela es más fuerte cerca del avión
                    const cerca = p < 1 ? Math.pow(u / cabeza, 2) : 0;
                    ctx.globalAlpha = (0.28 + 0.7 * cerca) * apagar;
                    ctx.strokeStyle = cerca > 0.6 ? '#ffffff' : '#b9a8ff';
                    ctx.lineWidth = (1.2 + 1.6 * cerca) * ui;
                    ctx.beginPath();
                    ctx.moveTo(prev.x, prev.y);
                    ctx.lineTo(actual.x, actual.y);
                    ctx.stroke();
                }
                prev = actual;
            }
            if (p < 1) dibujarAvion(ruta, cabeza, apagar);
        }
        ctx.restore();
    }

    /* ---------- avión visto desde arriba (nariz hacia +x) ---------- */
    const MITAD = [[54, 0], [50, -3.5], [43, -6], [14, -6], [-6, -44], [-17, -44], [-5, -6],
        [-35, -6], [-45, -19], [-52, -19], [-47, -4], [-51, 0]];
    const AVION = new Path2D();
    MITAD.forEach(([x, y], i) => (i === 0 ? AVION.moveTo(x, y) : AVION.lineTo(x, y)));
    for (let i = MITAD.length - 2; i > 0; i--) AVION.lineTo(MITAD[i][0], -MITAD[i][1]);
    AVION.closePath();

    function dibujarAvion(ruta, u, apagar) {
        const a = puntoRuta(ruta, Math.max(0, u - 0.01));
        const ax = a.x, ay = a.y;
        const q = puntoRuta(ruta, u);
        if (!q.visible) return;
        const ang = Math.atan2(q.y - ay, q.x - ax);
        // más grande a mitad de ruta (está más alto) y según el zoom
        const tam = limitar(cam.R / 1400, 0.16, 0.42) * ui * (0.8 + 0.5 * Math.sin(Math.PI * u));
        ctx.save();
        ctx.globalAlpha = (u < 0.06 ? u / 0.06 : u > 0.94 ? (1 - u) / 0.06 : 1) * apagar;
        ctx.translate(q.x, q.y);
        ctx.rotate(ang);
        ctx.scale(tam, tam);
        ctx.shadowColor = 'rgba(160, 130, 255, 0.95)';
        ctx.shadowBlur = 16;
        ctx.fillStyle = '#ffffff';
        ctx.fill(AVION);
        ctx.restore();
    }

    /* ---------- pines y ondas de llegada ---------- */
    function pin(X, Y, Z, color, radio, alfa) {
        const p = proyectar(X, Y, Z, 1);
        if (p.z <= 0.05 || alfa <= 0.01) return null;
        ctx.save();
        ctx.globalAlpha = alfa;
        ctx.fillStyle = color;
        ctx.shadowColor = color;
        ctx.shadowBlur = 12;
        ctx.beginPath();
        ctx.arc(p.x, p.y, radio * ui, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
        return { x: p.x, y: p.y, z: p.z };
    }

    function onda(x, y, p, color) {
        if (p <= 0 || p >= 1) return;
        ctx.save();
        ctx.globalAlpha = (1 - p) * 0.8;
        ctx.strokeStyle = color;
        ctx.lineWidth = 2 * ui;
        ctx.beginPath();
        ctx.arc(x, y, (4 + 34 * salida(p)) * ui, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();
    }

    function etiqueta(texto, x, y, alfa, grande, izquierda) {
        if (alfa <= 0.01) return;
        ctx.save();
        ctx.globalAlpha = alfa;
        ctx.font = (grande ? '700 ' + Math.round(20 * ui) : '600 ' + Math.round(12 * ui))
            + 'px Geist, "Helvetica Neue", Arial, sans-serif';
        ctx.textAlign = izquierda ? 'right' : 'left';
        ctx.fillStyle = '#ffffff';
        ctx.shadowColor = 'rgba(0,0,0,0.6)';
        ctx.shadowBlur = 6;
        ctx.fillText(texto, x + (izquierda ? -9 : grande ? 16 : 9) * ui, y - (grande ? 12 : 7) * ui);
        ctx.restore();
    }

    function dibujarLugares(t, apagar) {
        // destinos: se encienden al llegar el avión
        for (const ruta of rutas) {
            const llegada = ruta.inicio + T.arcoDur;
            const encendido = tramo(t, llegada - 150, llegada + 200);
            const p = pin(ruta.b[0], ruta.b[1], ruta.b[2], '#f5b83d', 3, Math.max(0.35, encendido) * apagar);
            if (!p) continue;
            onda(p.x, p.y, (t - llegada) / 900, '#f5b83d');
            if (p.z > 0.25) etiqueta(ruta.nombre, p.x, p.y, encendido * apagar * 0.9, false, ruta.izquierda);
        }
        // Valledupar: late todo el tiempo
        if (apagar <= 0.01) return;
        const o = pin(vOrigen[0], vOrigen[1], vOrigen[2], '#ffffff', 4.5, apagar);
        if (o) {
            onda(o.x, o.y, (t % 1300) / 1300, '#ffffff');
            const grande = t < T.alejarIni + 1400;
            etiqueta(grande ? 'Valledupar, Cesar' : 'Valledupar', o.x, o.y, apagar, grande);
        }
    }

    /* ---------- chispas que dejan los aviones ---------- */
    let chispas = [];
    let ultimaChispa = 0;
    const COLORES = ['#ffffff', '#b9a8ff', '#8051ff', '#f5b83d'];

    function crearChispas(t) {
        if (t - ultimaChispa < (movil ? 90 : 45)) return;
        ultimaChispa = t;
        for (const ruta of rutas) {
            const p = tramo(t, ruta.inicio, ruta.inicio + T.arcoDur);
            if (p <= 0.05 || p >= 0.95) continue;
            const q = puntoRuta(ruta, suave(p));
            if (!q.visible) continue;
            chispas.push({
                x: q.x + (Math.random() - 0.5) * 8, y: q.y + (Math.random() - 0.5) * 8,
                vx: (Math.random() - 0.5) * 0.02, vy: (Math.random() - 0.5) * 0.02,
                r: (Math.random() * 1.4 + 0.5) * ui, color: COLORES[(Math.random() * 4) | 0],
                nace: t, vida: 700 + Math.random() * 700
            });
        }
        if (chispas.length > 220) chispas.splice(0, chispas.length - 220);
    }

    function dibujarChispas(t, dt) {
        ctx.save();
        ctx.globalCompositeOperation = 'lighter';
        chispas = chispas.filter(c => t - c.nace < c.vida);
        for (const c of chispas) {
            c.x += c.vx * dt;
            c.y += c.vy * dt;
            const v = (t - c.nace) / c.vida;
            ctx.globalAlpha = (v < 0.2 ? v / 0.2 : 1 - v) * 0.8;
            ctx.fillStyle = c.color;
            ctx.fillRect(c.x - c.r, c.y - c.r, c.r * 2, c.r * 2);
        }
        ctx.restore();
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

        ctx.clearRect(0, 0, W, H);
        dibujarEstrellas(t);
        dibujarGlobo();
        crearChispas(t);
        dibujarChispas(t, dt);
        dibujarRutas(t, apagar);
        dibujarLugares(t, apagar);

        if (t > T.marca && raiz.dataset.fase !== 'marca') raiz.dataset.fase = 'marca';
        if (t >= T.duracion) terminar();
        if (!terminada || raiz.dataset.state === 'leaving') raf = requestAnimationFrame(cuadro);
    }

    function detener() {
        cancelAnimationFrame(raf);
        window.removeEventListener('resize', redimensionar);
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
