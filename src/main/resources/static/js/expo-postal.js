/*
 * Postal del viaje (idea 2, templates/expo/destino.html, Integrante 3).
 *
 * Se dibuja en el celular del visitante con <canvas> (1080 x 1350, formato vertical que WhatsApp muestra
 * grande): foto real del destino, "Saludos desde París", un pase de abordar con su nombre, un mini globo con
 * su ruta (js/globo.js) y un sello de la ExpoSoftware. Después la persona la comparte a su WhatsApp con el
 * menú "Compartir" del celular: gratis y sin límites, porque no la envía un proveedor sino ella misma.
 *
 * - La foto llega desde /expo/viaje/{id}/foto (mismo origen): así el canvas se puede exportar como imagen.
 * - Lleva el crédito de la foto: la licencia CC BY de Wikimedia lo exige.
 * - Si el celular no deja compartir archivos: "Descargar postal" y "Enviar solo el enlace" (wa.me).
 */
(function () {
    'use strict';

    const caja = document.getElementById('expo-postal');
    if (!caja) return;
    const d = caja.dataset;

    const ANCHO = 1080, ALTO = 1350, MARGEN = 60;
    const COLOR = {
        noche: '#0b0820', noche2: '#22186a', violeta: '#5433eb', lavanda: '#b9a8ff', ambar: '#f5b83d',
        tinta: '#0b0820', gris: '#7a7690'
    };
    const TITULO = '"Bricolage Grotesque", "Helvetica Neue", Arial, sans-serif';
    const TEXTO = 'Geist, "Helvetica Neue", Arial, sans-serif';
    const SERIF = '"Instrument Serif", Georgia, serif';
    const AVION = new Path2D('M54 0L50-3.5L43-6L14-6L-6-44L-17-44L-5-6L-35-6L-45-19L-52-19L-47-4L-51 0L-47 4' +
        'L-52 19L-45 19L-35 6L-5 6L-17 44L-6 44L14 6L43 6L50 3.5Z');

    const imagen = caja.querySelector('.expo-postal-imagen');
    const cargando = caja.querySelector('.expo-postal-cargando');
    const botonCompartir = caja.querySelector('[data-accion="compartir"]');
    const enlaceDescargar = caja.querySelector('[data-accion="descargar"]');
    const enlaceSolo = caja.querySelector('[data-accion="enlace"]');

    const mensaje = '✈️ Lancé mi viaje de Valledupar a ' + d.ciudad + ' en la ExpoSoftware con TravelGo. '
        + 'Únete a mi viaje: ' + d.enlace;
    const nombreArchivo = 'postal-travelgo-' + d.ciudad.normalize('NFD').replace(/[̀-ͯ]/g, '')
        .toLowerCase().replace(/[^a-z0-9]+/g, '-') + '.jpg';
    let archivo = null;
    let registrado = false;

    enlaceSolo.href = 'https://wa.me/?text=' + encodeURIComponent(mensaje);

    /* ---------- utilidades de dibujo ---------- */
    function cargarImagen(url) {
        return new Promise(resolver => {
            if (!url) return resolver(null);
            const img = new Image();
            img.onload = () => resolver(img);
            img.onerror = () => resolver(null);
            img.src = url;
        });
    }

    // Las letras de la web tardan en llegar; si no llegan en 2,5 s se dibuja con las del sistema
    function esperarFuentes() {
        if (!document.fonts) return Promise.resolve();
        const fuentes = Promise.all([
            document.fonts.load('800 120px "Bricolage Grotesque"'),
            document.fonts.load('italic 60px "Instrument Serif"'),
            document.fonts.load('600 30px Geist'),
            document.fonts.load('700 30px Geist')
        ]).catch(() => null);
        return Promise.race([fuentes, new Promise(r => setTimeout(r, 2500))]);
    }

    function rectRedondo(ctx, x, y, w, h, r) {
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + w, y, x + w, y + h, r);
        ctx.arcTo(x + w, y + h, x, y + h, r);
        ctx.arcTo(x, y + h, x, y, r);
        ctx.arcTo(x, y, x + w, y, r);
        ctx.closePath();
    }

    /** Escribe el texto con la letra más grande que quepa en maxAncho. */
    function textoAjustado(ctx, texto, x, y, maxAncho, fuente, tamMax, tamMin) {
        let tam = tamMax;
        ctx.font = fuente(tam);
        while (tam > tamMin && ctx.measureText(texto).width > maxAncho) {
            tam -= 2;
            ctx.font = fuente(tam);
        }
        let t = texto;
        while (t.length > 1 && ctx.measureText(t).width > maxAncho) t = t.slice(0, -2) + '…';
        ctx.fillText(t, x, y);
        return tam;
    }

    function avion(ctx, x, y, tam, angulo, color) {
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angulo);
        ctx.scale(tam / 108, tam / 108);
        ctx.fillStyle = color;
        ctx.fill(AVION);
        ctx.restore();
    }

    /* ---------- partes de la postal ---------- */
    function fondo(ctx, foto) {
        // Empieza oscuro justo donde termina la foto: así no se ve un corte de color
        const cielo = ctx.createLinearGradient(0, 900, 0, ALTO);
        cielo.addColorStop(0, COLOR.noche);
        cielo.addColorStop(1, COLOR.noche2);
        ctx.fillStyle = cielo;
        ctx.fillRect(0, 0, ANCHO, ALTO);

        const altoFoto = 900;
        if (foto) {
            // "cover": llena el área sin deformar la foto
            const escala = Math.max(ANCHO / foto.width, altoFoto / foto.height);
            const w = foto.width * escala, h = foto.height * escala;
            ctx.drawImage(foto, (ANCHO - w) / 2, (altoFoto - h) / 2, w, h);
        } else {
            const brillo = ctx.createRadialGradient(ANCHO / 2, 700, 50, ANCHO / 2, 700, 700);
            brillo.addColorStop(0, 'rgba(128, 81, 255, 0.7)');
            brillo.addColorStop(1, 'rgba(84, 51, 235, 0)');
            ctx.fillStyle = brillo;
            ctx.fillRect(0, 0, ANCHO, altoFoto);
        }
        // Velos para que el logo y la ciudad se lean sobre cualquier foto
        const arriba = ctx.createLinearGradient(0, 0, 0, 240);
        arriba.addColorStop(0, 'rgba(11, 8, 32, 0.6)');
        arriba.addColorStop(1, 'rgba(11, 8, 32, 0)');
        ctx.fillStyle = arriba;
        ctx.fillRect(0, 0, ANCHO, 240);
        const abajo = ctx.createLinearGradient(0, 420, 0, altoFoto);
        abajo.addColorStop(0, 'rgba(11, 8, 32, 0)');
        abajo.addColorStop(1, COLOR.noche);
        ctx.fillStyle = abajo;
        ctx.fillRect(0, 420, ANCHO, altoFoto - 420);
    }

    function logo(ctx) {
        rectRedondo(ctx, MARGEN, MARGEN, 68, 68, 18);
        ctx.fillStyle = COLOR.violeta;
        ctx.fill();
        avion(ctx, MARGEN + 34, MARGEN + 34, 40, -0.6, '#fff');
        ctx.fillStyle = '#fff';
        ctx.font = '700 42px ' + TITULO;
        ctx.textBaseline = 'middle';
        ctx.fillText('TravelGo', MARGEN + 88, MARGEN + 36);
        ctx.textBaseline = 'alphabetic';
    }

    function sello(ctx) {
        ctx.save();
        ctx.translate(ANCHO - 200, 150);
        ctx.rotate(-0.12);
        rectRedondo(ctx, -140, -78, 280, 156, 18);
        ctx.fillStyle = 'rgba(11, 8, 32, 0.55)';
        ctx.fill();
        ctx.setLineDash([12, 8]);
        ctx.lineWidth = 4;
        ctx.strokeStyle = COLOR.ambar;
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.textAlign = 'center';
        ctx.fillStyle = COLOR.ambar;
        ctx.font = '700 22px ' + TEXTO;
        ctx.fillText('EXPOSOFTWARE', 0, -36);
        ctx.fillStyle = '#fff';
        ctx.font = '800 46px ' + TITULO;
        ctx.textAlign = 'right';
        ctx.fillText('VUP', -26, 16);
        ctx.textAlign = 'left';
        ctx.fillText(d.aeropuerto, 26, 16);
        ctx.textAlign = 'center';
        avion(ctx, 0, 0, 34, 0, COLOR.ambar);
        ctx.font = '600 22px ' + TEXTO;
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.fillText(d.fecha || '', 0, 54);
        ctx.restore();
    }

    function titulo(ctx) {
        ctx.fillStyle = COLOR.lavanda;
        ctx.font = 'italic 68px ' + SERIF;
        ctx.fillText('Saludos desde', MARGEN, 690);
        ctx.fillStyle = '#fff';
        ctx.shadowColor = 'rgba(0, 0, 0, 0.45)';
        ctx.shadowBlur = 24;
        textoAjustado(ctx, d.ciudad, MARGEN - 4, 850, ANCHO - 2 * MARGEN,
            t => '800 ' + t + 'px ' + TITULO, 200, 90);
        ctx.shadowBlur = 0;
        ctx.fillStyle = 'rgba(255, 255, 255, 0.75)';
        ctx.font = '500 34px ' + TEXTO;
        ctx.fillText(d.pais, MARGEN, 905);
    }

    function credito(ctx) {
        if (!d.credito) return;
        ctx.textAlign = 'right';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.55)';
        textoAjustado(ctx, d.credito, ANCHO - MARGEN, 932, 560, t => '500 ' + t + 'px ' + TEXTO, 18, 14);
        ctx.textAlign = 'left';
    }

    function miniGlobo() {
        const G = window.TravelGoGlobo;
        if (!G) return null;
        const lienzo = document.createElement('canvas');
        const g = G.crear(lienzo, { medir: () => ({ ancho: 220, alto: 220 }) });
        if (!g) return null;
        const ruta = g.crearRuta({ lat: 10.4631, lon: -73.2532 }, { lat: +d.lat, lon: +d.lon });
        const centro = G.latLonRuta(ruta, 0.5);
        g.mirar({ lat: centro.lat, lon: centro.lon, R: 86, cx: 110, cy: 110 });
        g.limpiar();
        g.dibujarGlobo();
        g.dibujarEstela(ruta, 1, 1, false, 1.8);
        g.pin(ruta.a, '#ffffff', 4, 1);
        g.pin(ruta.b, COLOR.ambar, 4, 1);
        return lienzo;
    }

    function pase(ctx) {
        const x = MARGEN, y = 960, w = ANCHO - 2 * MARGEN, h = 250;
        ctx.save();
        ctx.shadowColor = 'rgba(0, 0, 0, 0.35)';
        ctx.shadowBlur = 40;
        ctx.shadowOffsetY = 14;
        rectRedondo(ctx, x, y, w, h, 34);
        ctx.fillStyle = '#fff';
        ctx.fill();
        ctx.restore();

        // Mini globo con la ruta, en una ventanita redonda oscura
        const globo = miniGlobo();
        ctx.save();
        ctx.beginPath();
        ctx.arc(x + 125, y + h / 2, 100, 0, Math.PI * 2);
        ctx.fillStyle = COLOR.noche;
        ctx.fill();
        ctx.clip();
        if (globo) ctx.drawImage(globo, x + 15, y + h / 2 - 110, 220, 220);
        ctx.restore();

        const col1 = x + 260, col2 = x + w - 250;
        const etiqueta = (texto, cx, cy) => {
            ctx.fillStyle = COLOR.gris;
            ctx.font = '700 21px ' + TEXTO;
            ctx.fillText(texto, cx, cy);
        };
        const valor = (texto, cx, cy, ancho) => {
            ctx.fillStyle = COLOR.tinta;
            textoAjustado(ctx, texto, cx, cy, ancho, t => '800 ' + t + 'px ' + TITULO, 46, 26);
        };

        etiqueta('PASAJERO', col1, y + 62);
        valor(d.nombre, col1, y + 110, col2 - col1 - 30);
        etiqueta('DISTANCIA', col2, y + 62);
        valor(d.km + ' km', col2, y + 110, 220);

        etiqueta('RUTA', col1, y + 165);
        ctx.fillStyle = COLOR.tinta;
        ctx.font = '800 46px ' + TITULO;
        ctx.fillText('VUP', col1, y + 213);
        const anchoVup = ctx.measureText('VUP').width;
        avion(ctx, col1 + anchoVup + 36, y + 197, 36, 0, COLOR.violeta);
        ctx.fillStyle = COLOR.tinta;
        ctx.fillText(d.aeropuerto, col1 + anchoVup + 72, y + 213);

        if (d.temperatura) {
            etiqueta('HOY ALLÁ', col2, y + 165);
            valor(d.temperatura + '° ' + (d.clima || ''), col2, y + 213, 220);
        }
    }

    function pie(ctx) {
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.font = '600 28px ' + TEXTO;
        ctx.fillText('Lancé este viaje en la ExpoSoftware', MARGEN, 1290);
        ctx.textAlign = 'right';
        ctx.fillStyle = COLOR.lavanda;
        ctx.font = 'italic 34px ' + SERIF;
        ctx.fillText('De Valledupar al mundo', ANCHO - MARGEN, 1290);
        ctx.textAlign = 'left';
    }

    async function dibujar() {
        const [foto] = await Promise.all([cargarImagen(d.foto), esperarFuentes()]);
        const lienzo = document.createElement('canvas');
        lienzo.width = ANCHO;
        lienzo.height = ALTO;
        const ctx = lienzo.getContext('2d');
        fondo(ctx, foto);
        logo(ctx);
        sello(ctx);
        titulo(ctx);
        credito(ctx);
        pase(ctx);
        pie(ctx);
        return new Promise(resolver => lienzo.toBlob(resolver, 'image/jpeg', 0.9));
    }

    /* ---------- compartir ---------- */
    function registrar() {
        if (registrado || d.propio !== 'true') return;
        registrado = true;
        const h = { 'Accept': 'application/json' };
        if (d.csrfHeader && d.csrfToken) h[d.csrfHeader] = d.csrfToken;
        fetch(d.api, { method: 'POST', headers: h }).catch(() => { registrado = false; });
    }

    function puedeCompartirArchivos() {
        return archivo && navigator.canShare && navigator.canShare({ files: [archivo] });
    }

    botonCompartir.addEventListener('click', async () => {
        if (puedeCompartirArchivos()) {
            try {
                await navigator.share({ files: [archivo], title: 'Mi viaje a ' + d.ciudad, text: mensaje });
                registrar();
            } catch (e) {
                // AbortError = la persona cerró el menú de compartir; no es un error
                if (e.name !== 'AbortError') window.open(enlaceSolo.href, '_blank', 'noopener');
            }
        } else {
            // Computador o navegador sin "compartir archivos": se descarga la postal y se abre WhatsApp
            enlaceDescargar.click();
            window.open(enlaceSolo.href, '_blank', 'noopener');
        }
    });
    enlaceDescargar.addEventListener('click', registrar);
    enlaceSolo.addEventListener('click', registrar);

    dibujar().then(blob => {
        if (!blob) throw new Error('sin imagen');
        archivo = new File([blob], nombreArchivo, { type: 'image/jpeg' });
        const url = URL.createObjectURL(blob);
        imagen.src = url;
        imagen.hidden = false;
        cargando.hidden = true;
        enlaceDescargar.href = url;
        enlaceDescargar.download = nombreArchivo;
        botonCompartir.disabled = false;
        if (!puedeCompartirArchivos()) botonCompartir.textContent = 'Descargar y enviar por WhatsApp';
    }).catch(() => {
        cargando.textContent = 'No se pudo dibujar la postal. Puedes enviar el enlace de tu viaje.';
        enlaceDescargar.hidden = true;
    });
})();
