/*
 * "Lanza tu viaje" en el celular (templates/expo/lanzar.html, Integrante 3).
 *
 * 1. Elegir nombre y destino → POST /api/expo/lanzamientos.
 * 2. Vuelo en vivo: un mini globo (js/globo.js) sigue al avión del visitante mientras el mismo vuelo cruza
 *    la pantalla del stand. Los km bajan hasta aterrizar.
 * 3. Al aterrizar pasa solo a /expo/viaje/{id}: el destino con datos reales y sus paquetes.
 *    Mientras vuela se pide esa página por adelantado: el vuelo esconde lo que tardan las APIs.
 *
 * El servidor valida el nombre y la espera entre lanzamientos; aquí solo se muestra su mensaje.
 */
(function () {
    'use strict';

    const app = document.getElementById('expo-lanzar');
    if (!app) return;

    const form = app.querySelector('[data-paso="elegir"]');
    const vuelo = app.querySelector('[data-paso="vuelo"]');
    const nombre = form.elements.nombre;
    const buscar = form.elements.buscar;
    const boton = form.querySelector('button[type="submit"]');
    const textoBoton = boton.querySelector('.expo-boton-texto');
    const error = form.querySelector('.expo-error');
    const sinResultados = form.querySelector('.expo-sin-resultados');
    const ORIGEN = { lat: 10.4631, lon: -73.2532 }; // Valledupar
    const CLAVE_NOMBRE = 'tg-expo-nombre';
    // La pantalla del stand pregunta cada 2,5 s: el celular vuela un poco más para aterrizar junto con ella
    const ESPERA_PANTALLA = 1800;

    let enviando = false;

    // El nombre se recuerda para el siguiente viaje (si el navegador no deja, no pasa nada)
    try { nombre.value = localStorage.getItem(CLAVE_NOMBRE) || ''; } catch (e) { /* sin almacenamiento */ }

    function seleccionado() {
        return form.querySelector('input[name="destino"]:checked');
    }

    function actualizarBoton() {
        const d = seleccionado();
        const conNombre = nombre.value.trim().length >= 2;
        boton.disabled = !(conNombre && d) || enviando;
        if (enviando) textoBoton.textContent = 'Despegando…';
        else if (!d) textoBoton.textContent = 'Elige un destino';
        else if (!conNombre) textoBoton.textContent = 'Escribe tu nombre';
        else textoBoton.textContent = 'Lanzar mi viaje a ' + d.dataset.ciudad;
    }

    function normalizar(texto) {
        return (texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
    }

    function filtrar() {
        const q = normalizar(buscar.value);
        let alguno = false;
        form.querySelectorAll('.expo-region').forEach(region => {
            let visibles = 0;
            region.querySelectorAll('.expo-destino').forEach(el => {
                const ver = !q || normalizar(el.dataset.busqueda).includes(q);
                el.hidden = !ver;
                if (ver) visibles++;
            });
            region.hidden = visibles === 0;
            if (visibles) alguno = true;
        });
        sinResultados.hidden = alguno;
    }

    // Distancia en línea recta (círculo máximo) desde Valledupar
    function distanciaKm(lat, lon) {
        const r = Math.PI / 180;
        const dLat = (lat - ORIGEN.lat) * r, dLon = (lon - ORIGEN.lon) * r;
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(ORIGEN.lat * r) * Math.cos(lat * r) * Math.sin(dLon / 2) ** 2;
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /* ---------- vuelo en vivo ---------- */
    function volar(viaje) {
        const destinoUrl = app.dataset.viaje + viaje.id;
        // Calienta la caché de foto, clima y atracciones mientras el avión vuela
        fetch(destinoUrl, { credentials: 'same-origin' }).catch(() => { /* se intenta de nuevo al llegar */ });

        vuelo.querySelectorAll('[data-campo="nombre"]').forEach(el => { el.textContent = viaje.nombre; });
        vuelo.querySelector('[data-campo="ciudad"]').textContent = viaje.ciudad;
        vuelo.querySelector('[data-campo="aeropuerto"]').textContent = viaje.aeropuerto || '';
        const estado = vuelo.querySelector('[data-campo="estado"]');
        const restante = vuelo.querySelector('[data-campo="restante"]');
        const avance = vuelo.querySelector('.expo-ruta-avance');
        const kmTotal = distanciaKm(viaje.latitud, viaje.longitud);

        form.hidden = true;
        vuelo.hidden = false;
        window.scrollTo({ top: 0 });
        if (navigator.vibrate) navigator.vibrate([30, 40, 60]);

        const G = window.TravelGoGlobo;
        const canvas = vuelo.querySelector('canvas');
        const lado = () => Math.min(vuelo.clientWidth || 320, 360);
        const g = G && G.crear(canvas, { medir: () => ({ ancho: lado(), alto: lado() }) });
        const destino = { lat: viaje.latitud, lon: viaje.longitud };
        const ruta = g ? g.crearRuta(ORIGEN, destino) : null;
        if (g) g.escalaAvion = 2.2; // en un globo pequeño el avión tiene que destacar
        const reducido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const duracion = reducido ? 1200 : ESPERA_PANTALLA + (ruta ? G.duracionVuelo(ruta) : 3000);
        let inicio = null;

        function cuadro(ahora) {
            if (inicio === null) inicio = ahora;
            const p = Math.min(1, (ahora - inicio) / duracion);
            const u = G ? G.suave(p) : p;

            restante.textContent = Math.round(kmTotal * (1 - u)).toLocaleString('es-CO');
            avance.style.transform = 'scaleX(' + u + ')';

            if (g) {
                // La cámara va un poco detrás del avión: se ve de dónde viene y hacia dónde va
                const cam = G.latLonRuta(ruta, Math.max(0, u - 0.08));
                g.mirar({ lat: cam.lat, lon: cam.lon, R: g.W * 0.4, cx: g.W / 2, cy: g.H / 2 });
                g.limpiar();
                g.dibujarEstrellas(ahora);
                g.dibujarGlobo();
                g.dibujarEstela(ruta, u, 1, p < 1);
                if (p < 1) g.dibujarAvion(ruta, u, 1);
                g.pin(ruta.a, '#ffffff', 4, 1);
                const llegada = g.pin(ruta.b, '#f5b83d', 4, 1);
                if (llegada && p >= 1) g.onda(llegada.x, llegada.y, ((ahora - inicio - duracion) % 1100) / 1100, '#f5b83d');
            }

            if (p < 1) {
                requestAnimationFrame(cuadro);
            } else {
                aterrizar(ahora);
            }
        }

        let aterrizo = false;
        function aterrizar() {
            if (aterrizo) return;
            aterrizo = true;
            estado.textContent = '¡Aterrizaste!';
            vuelo.classList.add('expo-aterrizo');
            if (navigator.vibrate) navigator.vibrate(80);
            // Un último cuadro con la onda de llegada y luego al destino
            if (g) requestAnimationFrame(function onda(t) {
                g.limpiar(); g.dibujarEstrellas(t); g.dibujarGlobo(); g.dibujarEstela(ruta, 1, 1, false);
                g.pin(ruta.a, '#ffffff', 4, 1);
                const q = g.pin(ruta.b, '#f5b83d', 4, 1);
                if (q) g.onda(q.x, q.y, ((t - inicio - duracion) % 1100) / 1100, '#f5b83d');
                if (!document.hidden) requestAnimationFrame(onda);
            });
            setTimeout(() => { window.location.href = destinoUrl; }, 1300);
        }

        requestAnimationFrame(cuadro);
    }

    async function lanzar(e) {
        e.preventDefault();
        const destino = seleccionado();
        if (!destino || enviando) return;
        enviando = true;
        error.textContent = '';
        actualizarBoton();

        const cabeceras = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (app.dataset.csrfHeader && app.dataset.csrfToken) cabeceras[app.dataset.csrfHeader] = app.dataset.csrfToken;

        try {
            const respuesta = await fetch(app.dataset.api, {
                method: 'POST',
                headers: cabeceras,
                body: JSON.stringify({ nombre: nombre.value.trim(), destinoId: destino.value })
            });
            const datos = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok) {
                throw new Error(datos.error || 'No se pudo lanzar el viaje. Intenta otra vez.');
            }
            try { localStorage.setItem(CLAVE_NOMBRE, datos.nombre); } catch (err) { /* sin almacenamiento */ }
            volar(datos);
        } catch (err) {
            // fetch lanza TypeError cuando no hay red ("Failed to fetch" en Chrome, "Load failed" en Safari)
            error.textContent = err instanceof TypeError
                ? 'Sin conexión con el stand. Revisa tu internet e intenta otra vez.' : err.message;
        } finally {
            enviando = false;
            actualizarBoton();
        }
    }

    form.addEventListener('submit', lanzar);
    form.addEventListener('change', actualizarBoton);
    nombre.addEventListener('input', () => { error.textContent = ''; actualizarBoton(); });
    buscar.addEventListener('input', filtrar);
    // En el celular, "Enter" en el buscador no debe enviar el formulario
    buscar.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); buscar.blur(); } });

    // Al volver con "Atrás" desde el destino, la página puede venir de la caché con el paso del vuelo abierto
    window.addEventListener('pageshow', e => {
        if (e.persisted) {
            vuelo.hidden = true;
            form.hidden = false;
            form.querySelectorAll('input[name="destino"]').forEach(r => { r.checked = false; });
            actualizarBoton();
        }
    });

    actualizarBoton();
})();
