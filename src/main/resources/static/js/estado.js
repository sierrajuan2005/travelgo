/*
 * Panel de estado de las APIs (templates/admin/estado.html, Integrante 3).
 * Pide /admin/estado/datos cada 3 s y pinta una tarjeta por API: semáforo (ícono + texto, nunca solo color),
 * latencia de la última llamada, gráfico de las últimas llamadas y los botones "Probar ahora" y "Simular caída".
 */
(function () {
    'use strict';

    const raiz = document.getElementById('tg-estado');
    if (!raiz) return;
    const plantilla = document.getElementById('tg-estado-tarjeta');
    const tarjetas = raiz.querySelector('[data-campo="tarjetas"]');
    const botonSinInternet = raiz.querySelector('[data-accion="sin-internet"]');
    const alertaSinInternet = raiz.querySelector('[data-campo="alerta-sin-internet"]');
    const CADA = 3000;

    const SEMAFORO = {
        VERDE: { texto: 'Bien', icono: 'bi-check-circle-fill' },
        AMARILLO: { texto: 'Atención', icono: 'bi-exclamation-circle-fill' },
        ROJO: { texto: 'Falló', icono: 'bi-x-circle-fill' },
        SIMULADA: { texto: 'Caída simulada', icono: 'bi-lightning-fill' },
        SIN_DATOS: { texto: 'Sin uso aún', icono: 'bi-dash-circle' }
    };
    const RESULTADO = { OK: 'respondió', VACIA: 'sin resultados', ERROR: 'falló', SIMULADA: 'caída simulada',
        CACHE: 'desde su caché' };

    let sinInternet = false;
    let ocupado = false;

    function cabeceras() {
        const h = { 'Accept': 'application/json' };
        if (raiz.dataset.csrfHeader && raiz.dataset.csrfToken) h[raiz.dataset.csrfHeader] = raiz.dataset.csrfToken;
        return h;
    }

    async function pedir(url, metodo) {
        const r = await fetch(url, { method: metodo || 'GET', headers: cabeceras() });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    }

    function hace(segundos) {
        if (segundos == null) return 'sin llamadas todavía';
        if (segundos < 5) return 'ahora mismo';
        if (segundos < 60) return 'hace ' + segundos + ' s';
        if (segundos < 3600) return 'hace ' + Math.round(segundos / 60) + ' min';
        return 'hace ' + Math.round(segundos / 3600) + ' h';
    }

    /* ---------- gráfico: una barra por llamada, alto = ms (escala propia de cada API) ---------- */
    const SVG = 'http://www.w3.org/2000/svg';
    function grafico(svg, puntos) {
        svg.replaceChildren();
        const ancho = 160, alto = 48, n = 20, hueco = 2;
        const barra = (ancho - hueco * (n - 1)) / n;
        const tope = Math.max(300, ...puntos.map(p => p.ms));
        // Línea base
        const base = document.createElementNS(SVG, 'line');
        base.setAttribute('x1', 0); base.setAttribute('x2', ancho);
        base.setAttribute('y1', alto - 0.5); base.setAttribute('y2', alto - 0.5);
        base.setAttribute('class', 'tg-estado-base');
        svg.append(base);
        // Las más nuevas a la derecha
        const desde = n - puntos.length;
        puntos.forEach((p, i) => {
            const fallo = p.resultado === 'ERROR' || p.resultado === 'SIMULADA';
            // Una falla no tiene un tiempo útil: barra corta fija para que se vea igual
            const h = fallo ? 10 : Math.max(3, (p.ms / tope) * (alto - 4));
            const x = (desde + i) * (barra + hueco);
            const rect = document.createElementNS(SVG, 'rect');
            rect.setAttribute('x', x.toFixed(1));
            rect.setAttribute('y', (alto - 1 - h).toFixed(1));
            rect.setAttribute('width', barra.toFixed(1));
            rect.setAttribute('height', h.toFixed(1));
            rect.setAttribute('rx', 2);
            rect.setAttribute('data-resultado', p.resultado);
            const titulo = document.createElementNS(SVG, 'title');
            titulo.textContent = (fallo ? '' : p.ms + ' ms · ') + RESULTADO[p.resultado];
            rect.append(titulo);
            svg.append(rect);
        });
        svg.setAttribute('aria-label', 'Últimas ' + puntos.length + ' llamadas');
    }

    /* ---------- tarjetas ---------- */
    function tarjeta(f) {
        let nodo = tarjetas.querySelector('[data-fuente="' + f.id + '"]');
        if (!nodo) {
            nodo = plantilla.content.firstElementChild.cloneNode(true);
            nodo.dataset.fuente = f.id;
            nodo.querySelector('[data-accion="probar"]').addEventListener('click', () => accion(f.id + '/probar'));
            nodo.querySelector('[data-accion="caida"]').addEventListener('click', e =>
                accion(f.id + '/caida?activa=' + (e.currentTarget.dataset.activa !== 'true')));
        }
        const campo = n => nodo.querySelector('[data-campo="' + n + '"]');
        campo('nombre').textContent = f.nombre;
        campo('para').textContent = f.paraQueSirve;
        campo('si-falla').textContent = f.siFalla;
        campo('cuota').textContent = 'Cuota: ' + f.cuota;

        const s = SEMAFORO[f.semaforo] || SEMAFORO.SIN_DATOS;
        const pill = campo('semaforo');
        pill.dataset.semaforo = f.semaforo;
        pill.querySelector('i').className = 'bi ' + s.icono;
        pill.querySelector('span').textContent = s.texto;
        nodo.querySelector('.tg-estado-tarjeta').dataset.semaforo = f.semaforo;

        // Si la última llamada falló, su tiempo no dice nada (pudo fallar antes de salir a internet)
        const sinTiempo = f.msUltima == null || f.semaforo === 'ROJO' || f.semaforo === 'SIMULADA';
        campo('ms').textContent = sinTiempo ? '—' : f.msUltima.toLocaleString('es-CO');
        const partes = [hace(f.haceSegundos)];
        if (f.llamadas) {
            partes.push(f.llamadas + (f.llamadas === 1 ? ' llamada' : ' llamadas'));
            partes.push(f.exito + ' % respondió');
            if (f.msPromedio != null) partes.push('promedio ' + f.msPromedio.toLocaleString('es-CO') + ' ms');
        }
        campo('detalle').textContent = partes.join(' · ');

        const error = campo('error');
        error.hidden = !f.ultimoError || f.semaforo === 'VERDE';
        error.textContent = f.ultimoError ? 'Último problema: ' + f.ultimoError : '';

        grafico(campo('grafico'), f.grafico);

        const probar = nodo.querySelector('[data-accion="probar"]');
        probar.disabled = !f.sePuedeProbar || f.caidaSimulada;
        probar.title = f.sePuedeProbar ? '' : 'No se prueba: gastaría cuota que no se recupera.';
        const caida = nodo.querySelector('[data-accion="caida"]');
        caida.dataset.activa = f.caidaSimulada;
        caida.className = 'btn btn-sm ' + (f.caidaSimulada ? 'btn-danger' : 'btn-outline-danger');
        caida.innerHTML = f.caidaSimulada
            ? '<i class="bi bi-plug-fill"></i> Reconectar'
            : '<i class="bi bi-lightning"></i> Simular caída';
        return nodo;
    }

    function pintar(datos) {
        sinInternet = datos.sinInternet;
        botonSinInternet.className = 'btn ' + (sinInternet ? 'btn-danger' : 'btn-outline-danger');
        botonSinInternet.querySelector('span').textContent = sinInternet ? 'Volver a conectar todo' : 'Simular sin internet';
        alertaSinInternet.hidden = !sinInternet;
        raiz.querySelector('[data-campo="cuantas"]').textContent = datos.fuentes.length;
        const nodos = datos.fuentes.map(tarjeta);
        tarjetas.replaceChildren(...nodos);
    }

    async function accion(ruta) {
        if (ocupado) return;
        ocupado = true;
        raiz.classList.add('tg-estado-ocupado');
        try {
            pintar(await pedir(raiz.dataset.base + ruta, 'POST'));
        } catch (e) {
            // La próxima actualización lo intenta de nuevo
        } finally {
            ocupado = false;
            raiz.classList.remove('tg-estado-ocupado');
        }
    }

    async function actualizar() {
        if (!ocupado && !document.hidden) {
            try { pintar(await pedir(raiz.dataset.datos)); } catch (e) { /* se reintenta */ }
        }
        setTimeout(actualizar, CADA);
    }

    raiz.querySelector('[data-accion="probar-todas"]').addEventListener('click', () => accion('probar-todas'));
    botonSinInternet.addEventListener('click', () => accion('sin-internet?activa=' + !sinInternet));
    actualizar();
})();
