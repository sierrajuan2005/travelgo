/*
 * Página del destino al aterrizar (templates/expo/destino.html, Integrante 3).
 *
 * 1. Guardar o borrar el celular para la postal (el número completo nunca vuelve al navegador).
 * 2. WhatsApp: WhatsApp no deja que una empresa escriba primero, así que el visitante toca "Activar WhatsApp"
 *    y envía el mensaje que aparece escrito. Cuando vuelve a esta página, se le envía su viaje solo
 *    (o con "Ya lo activé"), y aquí se ve el estado real: enviando → entregado.
 * El servidor valida todo (número, autorización, que el viaje sea de este celular); aquí solo se muestra.
 */
(function () {
    'use strict';

    const caja = document.getElementById('expo-contacto');
    if (!caja) return;

    const form = caja.querySelector('[data-estado="pedir"]');
    const listo = caja.querySelector('[data-estado="guardado"]');
    const error = form.querySelector('.expo-error');
    const boton = form.querySelector('button[type="submit"]');
    const estadoWa = caja.querySelector('[data-campo="estado-wa"]');
    const botonEnviar = caja.querySelector('[data-accion="enviar"]');
    const conWhatsApp = caja.dataset.whatsapp === 'true';

    const TEXTOS = {
        ENVIANDO: 'Enviando tu viaje por WhatsApp…',
        ENTREGADO: '✓ ¡Llegó! Revisa tu WhatsApp.',
        LEIDO: '✓ ¡Llegó! Revisa tu WhatsApp.',
        SIN_ACTIVAR: 'Todavía no activaste WhatsApp: toca «Activar WhatsApp» y envía el mensaje que aparece.',
        FALLIDO: 'No se pudo entregar. Intenta otra vez.'
    };

    let activo = false;          // tocó "Activar WhatsApp" y todavía no se le ha enviado
    let reintentos = 0;          // reintentos automáticos si el sandbox aún no registraba su activación
    let consultando = 0;

    function cabeceras() {
        const h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (caja.dataset.csrfHeader && caja.dataset.csrfToken) h[caja.dataset.csrfHeader] = caja.dataset.csrfToken;
        return h;
    }

    async function pedir(url, metodo, cuerpo) {
        const r = await fetch(url, {
            method: metodo, headers: cabeceras(), body: cuerpo ? JSON.stringify(cuerpo) : undefined
        });
        const datos = r.status === 204 ? null : await r.json().catch(() => ({}));
        if (!r.ok) throw new Error((datos && datos.error) || 'No se pudo completar. Intenta otra vez.');
        return datos;
    }

    function mostrar(guardado) {
        form.hidden = guardado;
        listo.hidden = !guardado;
    }

    function pintarEstado(estado) {
        estadoWa.textContent = TEXTOS[estado] || '';
        estadoWa.dataset.estado = estado || '';
        if (botonEnviar) botonEnviar.disabled = estado === 'ENVIANDO' || estado === 'ENTREGADO' || estado === 'LEIDO';
    }

    /* ---------- celular ---------- */
    form.addEventListener('submit', async e => {
        e.preventDefault();
        error.textContent = '';
        if (!form.elements.autoriza.checked) {
            error.textContent = 'Marca la autorización para que podamos escribirte.';
            return;
        }
        boton.disabled = true;
        try {
            const datos = await pedir(caja.dataset.api, 'POST', {
                telefono: form.elements.telefono.value, autoriza: form.elements.autoriza.checked
            });
            listo.querySelector('[data-campo="telefono"]').textContent = datos.telefono;
            mostrar(true);
            pintarEstado('');
            if (navigator.vibrate) navigator.vibrate(40);
        } catch (err) {
            error.textContent = err instanceof TypeError ? 'Sin conexión. Intenta otra vez.' : err.message;
        } finally {
            boton.disabled = false;
        }
    });

    listo.querySelector('[data-accion="borrar"]').addEventListener('click', async () => {
        try {
            await pedir(caja.dataset.api, 'DELETE');
            form.reset();
            pintarEstado('');
            mostrar(false);
        } catch (err) {
            estadoWa.textContent = err.message;
        }
    });

    /* ---------- WhatsApp ---------- */
    async function enviarWhatsApp() {
        activo = false;
        pintarEstado('ENVIANDO');
        try {
            const r = await pedir(caja.dataset.apiWhatsapp, 'POST');
            pintarEstado(r.estado);
            seguir();
        } catch (err) {
            estadoWa.textContent = err instanceof TypeError ? 'Sin conexión. Intenta otra vez.' : err.message;
            if (botonEnviar) botonEnviar.disabled = false;
        }
    }

    // Pregunta el estado real cada 2 s (hasta ~40 s) mientras el mensaje va en camino
    function seguir() {
        const turno = ++consultando;
        let veces = 0;
        const mirar = async () => {
            if (turno !== consultando) return;
            try {
                const r = await pedir(caja.dataset.apiWhatsapp, 'GET');
                if (!r) return;
                pintarEstado(r.estado);
                if (r.estado === 'SIN_ACTIVAR' && reintentos < 2) {
                    // Pudo enviar antes de que WhatsApp registrara la activación: se intenta solo otra vez
                    reintentos++;
                    setTimeout(enviarWhatsApp, 4000);
                    return;
                }
                if (r.estado !== 'ENVIANDO') return;
            } catch (err) { /* se intenta en la próxima vuelta */ }
            if (++veces < 20) setTimeout(mirar, 2000);
        };
        setTimeout(mirar, 1500);
    }

    if (conWhatsApp) {
        caja.querySelector('[data-accion="activar"]').addEventListener('click', () => {
            activo = true;
            reintentos = 0;
        });
        botonEnviar.addEventListener('click', () => { reintentos = 0; enviarWhatsApp(); });

        // Al volver de WhatsApp a esta página, se envía solo
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible' && activo && !listo.hidden) {
                setTimeout(enviarWhatsApp, 1500);
            }
        });

        // Si ya se había enviado antes (recargó la página), se muestra cómo va
        const anterior = caja.dataset.estadoWhatsapp;
        if (anterior) {
            pintarEstado(anterior);
            if (anterior === 'ENVIANDO' || anterior === 'ENTREGADO') seguir();
        }
    }
})();
