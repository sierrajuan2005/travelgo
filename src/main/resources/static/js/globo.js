/*
 * Globo terráqueo 3D de TravelGo (Integrante 3), compartido por la intro (js/intro.js) y la pantalla
 * de la ExpoSoftware (js/expo-pantalla.js).
 *
 * - Proyección ortográfica dibujada en <canvas>, sin librerías.
 * - Los continentes son una máscara de puntos cada 2° sacada de Natural Earth (land 110m, dominio público),
 *   guardada aquí en base64: el globo no depende de internet.
 * - Quien lo usa mueve la cámara (mirar) y decide qué dibujar en cada cuadro; este archivo solo sabe dibujar.
 *
 * Uso:
 *   const g = TravelGoGlobo.crear(canvas);       // null si el navegador no tiene canvas 2D
 *   TravelGoGlobo.crear(canvas, { medir: () => ({ ancho: 300, alto: 300 }) })  // lienzo que no es toda la ventana
 *   g.mirar({ lat, lon, R, cx, cy });            // cada cuadro
 *   g.limpiar(); g.dibujarEstrellas(t); g.dibujarGlobo();
 *   const ruta = g.crearRuta(origen, destino);   // { lat, lon } -> arco de círculo máximo
 *   g.dibujarEstela(ruta, avance, alfa); g.dibujarAvion(ruta, avance, alfa);
 */
(function () {
    'use strict';

    const RAD = Math.PI / 180;

    /* ---------- utilidades ---------- */
    const limitar = (v, a, b) => Math.max(a, Math.min(b, v));
    const lerp = (a, b, t) => a + (b - a) * t;
    const suave = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
    const salida = t => 1 - Math.pow(1 - t, 3);
    const tramo = (t, a, b) => limitar((t - a) / (b - a), 0, 1);

    const aVector = (lat, lon) => {
        const f = lat * RAD, l = lon * RAD;
        return [Math.cos(f) * Math.cos(l), Math.cos(f) * Math.sin(l), Math.sin(f)];
    };

    /* ---------- máscara de tierra: 180 x 90 celdas de 2° (bit = tierra) ---------- */
    const MASCARA = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOAfAP8DAAAAAAAAAAAAAAAAAAAAAADo//z//wcAAAAAAAACAAAAAAAAAAAAhvvw//8PAPABAAAAwAMAAAAAAAAAwADkw////wEABAAAAABgAAAAAAAAAADAUT8A/v8PAAAAAAwA/j8AuAEAAAAAcBHtDcD/fwAAAAAwAPz/fwMAABCAAQD5w/wD8P8FAAAMAIL7//////9/AP//////////////B/D/SAAAAAAAQID//////////////x8AKAAEAAAAAAAA8///////////////YCgAAAAAAAAAAADw/////wEs4AEAAHz+////////////gL////8HeAAcAADg5///////////9ADgAf7/f4AnAAAAAH78////////H0QAAAiA//8f8AcAAIBB4////////38ADwAQAOD//5//AQAAHAT/////////A3AAAAAA/v//+T8AAGDz//////////8DAQAAAMD/////AwAAsP//////////LwAAAAAA6P///2IAAAD+//////////8CAAAAAAD///8/CAAA4P//////////JwAAAAAA8P///wYAAAD+/unz/////z8AAAAAAAD///8HAAAA/pgPPP//////MQAAAAAA8P//PwAAAMBD9v7n/////wcBAAAAAAD///8AAAAAPkD7f/7///8hEAAAAAAA4P//DwAAAIDhAv/n////f8YAAAAAAAD8//8AAAAA+AdE//////8jDwAAAAAAgP//AwAAAMD/APD/////PxgAAAAAAADw/x8AAAAA/n/v//////8HAAAAAAAAAPwDAgAAAOD////7////fwAAAAAAAACgHyAAAACA//9/f/7///8DAAAAAAAAAPQBAAAAAPj//+cv+P//PwAAAAAAAAAAHjAAAADA/////g/+//8EAAAAAAAAAOBhCAAAAP7//99/4D//AAAAAAAAAAAAPAMEAADA////+Qf84BcAAAAAAAAAAAA/AAAAAPz//58fgAf+QAAAAAAAAAAAAA8AAADg////ewA4gA8EAAAAAAAAAADAAAAAAPz//38BgAP4QQAAAAAAAAAAAAgPAADA////zwAwgAwQAAAAAAAAAAAA9Q8AAPj///8HAAVIAAAAAAAAAAAAAID/AQAA////fwBAAAAQAAAAAAAAAAAA+P8AAGDh//8DAAA0GAAAAAAAAAAAAID/HwAAAPj/HwAAgMIBAAAAAAAAAAAA/P8BAACA//8AAAAYXgAAAAAAAAAAAMD/fwAAAPz/BwAAAOOBAQAAAAAAAAAA/P8/AACA/z8AAABgbtQBAAAAAAAAAOD//w8AAPD/AwAAAAQIeAAAAAAAAAAA/P//AQAA/z8AAACAA4APAQAAAAAAAID//w8AAPD/AwAAAAARsEAAAAAAAAAA+P9/AAAA/j8AAAAAAAAAAAAAAAAAAAD//wcAAPD/QwAAAACAIwAAAAAAAAAA8P9/AAAA/z8EAAAAAD8GIAAAAAAAAAD8/wMAAPD/cQAAAAD4ZwAAAAAAAAAAgP8/AAAA/w8HAAAAgP8HAAAAAAAAAAD4/wMAAOD/MAAAAAD//wEBAAAAAAAAgP8PAAAA/g8DAAAA+P8fAAAAAAAAAAD4PwAAAOB/EAAAAID//wMAAAAAAAAAgP8DAAAA/AMAAAAA+P9/AAAAAAAAAAD8HwAAAMA/AAAAAID//wcAAAAAAAAAwP8BAAAA+AEAAAAA8P9/AAAAAAAAAAD8DwAAAIAPAAAAAAAP/gMAAAAAAAAAwB8AAAAAAAAAAAAAEIAfAAEAAAAAAAD+AwAAAAAAAAAAAAAA8AEgAAAAAAAA4AcAAAAAAAAAAAAAAAAAAAYAAAAAAABeAAAAAAAAAAAAAAAAwAAwAAAAAAAAwAMAAAAAAAAAAAAAAAAIgAEAAAAAAAAeAAAAAAAAAAAAAAAAAAAMAAAAAAAA4AEAAAAAAAAAAAAAAAAAAAAAAAAAAAAPAAAAAAAAAAABAAAAAAAAAAAAAAAAcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOAAAAAAAAAAAAAAAAAAAAAAAAAAAAwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAAAAB4AEDwn/8HAAAAAAAAAAAwAAAAAACA/P/h/////w8AAAAAAAAAwAcAAAD4////z///////PwAAAAAAHALwAACA//////////////8HAADw/y///wMAAP7/////////////HwAA+P///38AAID///////////////8AAPL/////BwAO////////////////DwAA8P////8HEPD//////////////z8AAOD/////////////////////////HwDgPwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';
    const COLS = 180, FILAS = 90, RES = 2;

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

    /* ---------- avión visto desde arriba (nariz hacia +x) ---------- */
    const MITAD = [[54, 0], [50, -3.5], [43, -6], [14, -6], [-6, -44], [-17, -44], [-5, -6],
        [-35, -6], [-45, -19], [-52, -19], [-47, -4], [-51, 0]];
    const AVION = new Path2D();
    MITAD.forEach(([x, y], i) => (i === 0 ? AVION.moveTo(x, y) : AVION.lineTo(x, y)));
    for (let i = MITAD.length - 2; i > 0; i--) AVION.lineTo(MITAD[i][0], -MITAD[i][1]);
    AVION.closePath();

    const MUESTRAS = 64;
    const COLORES_CHISPA = ['#ffffff', '#b9a8ff', '#8051ff', '#f5b83d'];

    function crear(canvas, opciones) {
        const ctx = canvas && canvas.getContext('2d');
        if (!ctx) return null;
        // Por defecto el lienzo ocupa toda la ventana (intro y pantalla del stand)
        const medir = (opciones && opciones.medir) || (() => ({ ancho: window.innerWidth, alto: window.innerHeight }));

        // escalaAvion: la pantalla del stand la sube para que los aviones se vean desde lejos
        const g = { canvas, ctx, W: 0, H: 0, M: 0, ui: 1, movil: false, escalaAvion: 1 };
        const cam = { cosL: 1, sinL: 0, cosP: 1, sinP: 0, R: 1, cx: 0, cy: 0 };
        g.cam = cam;

        /* ---------- lienzo ---------- */
        g.redimensionar = function () {
            const dpr = Math.min(window.devicePixelRatio || 1, 1.5);
            const medida = medir();
            g.W = medida.ancho;
            g.H = medida.alto;
            g.M = Math.min(g.W, g.H);
            canvas.width = Math.round(g.W * dpr);
            canvas.height = Math.round(g.H * dpr);
            canvas.style.width = g.W + 'px';
            canvas.style.height = g.H + 'px';
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
            g.ui = Math.max(0.7, g.M / 820);
            g.movil = g.W < 640;
        };
        g.redimensionar();

        g.limpiar = () => ctx.clearRect(0, 0, g.W, g.H);

        /* ---------- cámara: el punto (lat, lon) queda en el centro (cx, cy) y el globo mide R ---------- */
        g.mirar = function (v) {
            cam.cosL = Math.cos(v.lon * RAD); cam.sinL = Math.sin(v.lon * RAD);
            cam.cosP = Math.cos(v.lat * RAD); cam.sinP = Math.sin(v.lat * RAD);
            cam.R = v.R; cam.cx = v.cx; cam.cy = v.cy;
        };

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
        g.proyectar = proyectar;

        /* ---------- estrellas con un poco de paralaje ---------- */
        const estrellas = Array.from({ length: 140 }, () => ({
            x: Math.random(), y: Math.random(), r: Math.random() * 1.2 + 0.3, fase: Math.random() * 6.3
        }));

        g.dibujarEstrellas = function (t) {
            const desplazar = (t / 1000) * 9;
            ctx.fillStyle = '#ffffff';
            for (const e of estrellas) {
                ctx.globalAlpha = 0.2 + 0.4 * (0.5 + 0.5 * Math.sin(t / 650 + e.fase));
                const x = ((e.x * g.W - desplazar * e.r) % g.W + g.W) % g.W;
                ctx.fillRect(x, e.y * g.H, e.r, e.r);
            }
            ctx.globalAlpha = 1;
        };

        /* ---------- globo: atmósfera, océano, meridianos y tierra ---------- */
        g.dibujarGlobo = function () {
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
            const paso = g.movil ? 2 : 1;
            ctx.fillStyle = '#cfc4ff';
            for (let k = 0; k < tierra.length; k += 3 * paso) {
                const p = proyectar(tierra[k], tierra[k + 1], tierra[k + 2], 1);
                if (p.z <= 0.02 || p.x < -10 || p.x > g.W + 10 || p.y < -10 || p.y > g.H + 10) continue;
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
        };

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
        g.crearRuta = function (origen, destino) {
            const a = aVector(origen.lat, origen.lon);
            const b = aVector(destino.lat, destino.lon);
            const omega = Math.acos(limitar(a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1, 1));
            const alto = 0.05 + 0.3 * (omega / Math.PI);
            const seno = Math.sin(omega) || 1e-6;
            const pts = [];
            for (let s = 0; s <= MUESTRAS; s++) {
                const u = s / MUESTRAS;
                const k1 = omega < 1e-6 ? 1 - u : Math.sin((1 - u) * omega) / seno;
                const k2 = omega < 1e-6 ? u : Math.sin(u * omega) / seno;
                pts.push({
                    X: k1 * a[0] + k2 * b[0], Y: k1 * a[1] + k2 * b[1], Z: k1 * a[2] + k2 * b[2],
                    h: 1 + alto * Math.sin(Math.PI * u)
                });
            }
            return { a, b, pts, omega };
        };

        function puntoRuta(ruta, u) {
            const f = u * MUESTRAS, i = Math.min(MUESTRAS - 1, Math.floor(f)), r = f - i;
            const a = ruta.pts[i], b = ruta.pts[i + 1];
            return proyectar(lerp(a.X, b.X, r), lerp(a.Y, b.Y, r), lerp(a.Z, b.Z, r), lerp(a.h, b.h, r));
        }
        g.puntoRuta = puntoRuta;

        /**
         * Dibuja la estela de una ruta hasta "cabeza" (0..1).
         * enVuelo = true mientras el avión no ha llegado: la estela es más fuerte cerca del avión.
         * fuerza escala el grosor y la opacidad de las rutas ya terminadas (la pantalla de la expo pinta cientos).
         */
        g.dibujarEstela = function (ruta, cabeza, alfa, enVuelo, fuerza) {
            if (alfa <= 0.01 || cabeza <= 0) return;
            const base = fuerza === undefined ? 1 : fuerza;
            ctx.save();
            ctx.lineCap = 'round';
            const n = Math.max(2, Math.round(MUESTRAS * cabeza));
            let prevX = 0, prevY = 0, prevVisible = false;
            for (let s = 0; s <= n; s++) {
                const u = (s / n) * cabeza;
                const q = puntoRuta(ruta, u);
                if (s > 0 && prevVisible && q.visible) {
                    const cerca = enVuelo ? Math.pow(u / cabeza, 2) : 0;
                    ctx.globalAlpha = (0.28 * base + 0.7 * cerca) * alfa;
                    ctx.strokeStyle = cerca > 0.6 ? '#ffffff' : '#b9a8ff';
                    ctx.lineWidth = (1.2 * Math.max(base, 0.6) + 1.6 * cerca) * g.ui;
                    ctx.beginPath();
                    ctx.moveTo(prevX, prevY);
                    ctx.lineTo(q.x, q.y);
                    ctx.stroke();
                }
                prevX = q.x; prevY = q.y; prevVisible = q.visible;
            }
            ctx.restore();
        };

        /** escala (opcional): tamaño de este avión respecto a los demás (un vuelo de grupo es más grande). */
        g.dibujarAvion = function (ruta, u, alfa, escala) {
            const a = puntoRuta(ruta, Math.max(0, u - 0.01));
            const ax = a.x, ay = a.y;
            const q = puntoRuta(ruta, u);
            if (!q.visible) return;
            const ang = Math.atan2(q.y - ay, q.x - ax);
            // más grande a mitad de ruta (está más alto) y según el zoom
            const tam = limitar(cam.R / 1400, 0.16, 0.42) * g.ui * g.escalaAvion * (escala || 1) * (0.8 + 0.5 * Math.sin(Math.PI * u));
            ctx.save();
            ctx.globalAlpha = (u < 0.06 ? u / 0.06 : u > 0.94 ? (1 - u) / 0.06 : 1) * alfa;
            ctx.translate(q.x, q.y);
            ctx.rotate(ang);
            ctx.scale(tam, tam);
            ctx.shadowColor = 'rgba(160, 130, 255, 0.95)';
            ctx.shadowBlur = 16;
            ctx.fillStyle = '#ffffff';
            ctx.fill(AVION);
            ctx.restore();
        };

        /* ---------- pines, ondas y etiquetas ---------- */
        g.pin = function (v, color, radio, alfa) {
            const p = proyectar(v[0], v[1], v[2], 1);
            if (p.z <= 0.05 || alfa <= 0.01) return null;
            ctx.save();
            ctx.globalAlpha = alfa;
            ctx.fillStyle = color;
            ctx.shadowColor = color;
            ctx.shadowBlur = 12;
            ctx.beginPath();
            ctx.arc(p.x, p.y, radio * g.ui, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
            return { x: p.x, y: p.y, z: p.z };
        };

        g.onda = function (x, y, p, color) {
            if (p <= 0 || p >= 1) return;
            ctx.save();
            ctx.globalAlpha = (1 - p) * 0.8;
            ctx.strokeStyle = color;
            ctx.lineWidth = 2 * g.ui;
            ctx.beginPath();
            ctx.arc(x, y, (4 + 34 * salida(p)) * g.ui, 0, Math.PI * 2);
            ctx.stroke();
            ctx.restore();
        };

        g.etiqueta = function (texto, x, y, alfa, grande, izquierda) {
            if (alfa <= 0.01) return;
            ctx.save();
            ctx.globalAlpha = alfa;
            ctx.font = (grande ? '700 ' + Math.round(20 * g.ui) : '600 ' + Math.round(12 * g.ui))
                + 'px Geist, "Helvetica Neue", Arial, sans-serif';
            ctx.textAlign = izquierda ? 'right' : 'left';
            ctx.fillStyle = '#ffffff';
            ctx.shadowColor = 'rgba(0,0,0,0.6)';
            ctx.shadowBlur = 6;
            ctx.fillText(texto, x + (izquierda ? -9 : grande ? 16 : 9) * g.ui, y - (grande ? 12 : 7) * g.ui);
            ctx.restore();
        };

        /* ---------- chispas que dejan los aviones ---------- */
        let chispas = [];
        g.soltarChispa = function (x, y, t) {
            chispas.push({
                x: x + (Math.random() - 0.5) * 8, y: y + (Math.random() - 0.5) * 8,
                vx: (Math.random() - 0.5) * 0.02, vy: (Math.random() - 0.5) * 0.02,
                r: (Math.random() * 1.4 + 0.5) * g.ui, color: COLORES_CHISPA[(Math.random() * 4) | 0],
                nace: t, vida: 700 + Math.random() * 700
            });
            if (chispas.length > 220) chispas.splice(0, chispas.length - 220);
        };

        g.dibujarChispas = function (t, dt) {
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
        };

        return g;
    }

    /**
     * Cuánto dura un vuelo en la pantalla del stand (ms): más lejos, más largo, pero siempre corto para que
     * haya movimiento todo el tiempo. El celular usa la misma cuenta para aterrizar junto con la pantalla.
     */
    const duracionVuelo = ruta => 1300 + 1500 * (ruta.omega / Math.PI);

    /** Latitud y longitud (grados) del punto u (0..1) de una ruta: para que una cámara siga al avión. */
    function latLonRuta(ruta, u) {
        // Interpola entre muestras para que la cámara se mueva suave, no a saltos
        const f = limitar(u, 0, 1) * MUESTRAS, i = Math.min(MUESTRAS - 1, Math.floor(f)), r = f - i;
        const a = ruta.pts[i], b = ruta.pts[i + 1];
        const X = lerp(a.X, b.X, r), Y = lerp(a.Y, b.Y, r), Z = lerp(a.Z, b.Z, r);
        const n = Math.hypot(X, Y, Z) || 1;
        return { lat: Math.asin(limitar(Z / n, -1, 1)) / RAD, lon: Math.atan2(Y, X) / RAD };
    }

    window.TravelGoGlobo = { crear, aVector, limitar, lerp, suave, salida, tramo, duracionVuelo, latLonRuta };
})();
