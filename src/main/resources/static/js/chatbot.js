/*
 * Widget del chatbot de TravelGo (Sprint 5, Integrante 3).
 * Habla con el motor del Integrante 4 según docs/chatbot_contrato.md:
 *   POST /api/chatbot                       { sesionId, mensaje } -> { sesionId, respuesta, sugerencias }
 *   GET  /api/chatbot/{sesionId}/mensajes   -> [{ emisor, texto, ... }]
 * Las respuestas son texto plano: se muestran con textContent (nunca como HTML) y respetando los saltos de línea.
 */
(function () {
    'use strict';

    const raiz = document.getElementById('tg-chat');
    if (!raiz) {
        return;
    }

    const URL_CHAT = raiz.dataset.url;
    const CSRF_HEADER = raiz.dataset.csrfHeader;
    const CSRF_TOKEN = raiz.dataset.csrfToken;
    const CLAVE_SESION = 'tg-chat-sesion';
    const SUGERENCIAS_INICIALES = [
        'Paquetes baratos', 'Paquetes de lujo', '¿Hay cupones?', '¿Cómo pago?', '¿Cómo va mi reserva?'
    ];
    const SALUDO = 'Hola, soy el asistente de TravelGo. Puedo buscarte paquetes por destino o precio, '
        + 'decirte cómo va tu reserva, si tienes pagos pendientes y qué cupones hay. ¿En qué te ayudo?';

    const boton = document.getElementById('tg-chat-boton');
    const ventana = document.getElementById('tg-chat-ventana');
    const mensajes = document.getElementById('tg-chat-mensajes');
    const sugerencias = document.getElementById('tg-chat-sugerencias');
    const formulario = document.getElementById('tg-chat-formulario');
    const campo = document.getElementById('tg-chat-texto');
    const nueva = document.getElementById('tg-chat-nueva');

    let iniciado = false;
    let enviando = false;

    // ---------- Sesión de la conversación (se guarda solo en esta pestaña) ----------

    function leerSesion() {
        try {
            return sessionStorage.getItem(CLAVE_SESION);
        } catch (e) {
            return null;
        }
    }

    function guardarSesion(id) {
        try {
            if (id) {
                sessionStorage.setItem(CLAVE_SESION, id);
            } else {
                sessionStorage.removeItem(CLAVE_SESION);
            }
        } catch (e) {
            // Sin sessionStorage la conversación vive solo mientras la página esté abierta.
        }
    }

    let sesionId = leerSesion();

    // ---------- Pintar mensajes y botones ----------

    function agregarMensaje(texto, emisor) {
        const burbuja = document.createElement('div');
        burbuja.className = 'tg-chat-msg ' + (emisor === 'USUARIO' ? 'tg-chat-msg-usuario' : 'tg-chat-msg-bot');
        burbuja.textContent = texto;
        mensajes.appendChild(burbuja);
        mensajes.scrollTop = mensajes.scrollHeight;
        return burbuja;
    }

    function mostrarEscribiendo() {
        const burbuja = document.createElement('div');
        burbuja.className = 'tg-chat-msg tg-chat-msg-bot tg-chat-escribiendo';
        burbuja.setAttribute('aria-label', 'El asistente está escribiendo');
        burbuja.innerHTML = '<span></span><span></span><span></span>';
        mensajes.appendChild(burbuja);
        mensajes.scrollTop = mensajes.scrollHeight;
        return burbuja;
    }

    function mostrarSugerencias(lista) {
        sugerencias.replaceChildren();
        (lista || []).forEach(function (texto) {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'tg-chat-sugerencia';
            b.textContent = texto;
            b.addEventListener('click', function () {
                enviar(texto);
            });
            sugerencias.appendChild(b);
        });
    }

    function saludar() {
        agregarMensaje(SALUDO, 'BOT');
        mostrarSugerencias(SUGERENCIAS_INICIALES);
    }

    // ---------- Llamadas al servidor ----------

    function cabeceras() {
        const h = {'Content-Type': 'application/json', 'Accept': 'application/json'};
        if (CSRF_HEADER && CSRF_TOKEN) {
            h[CSRF_HEADER] = CSRF_TOKEN;
        }
        return h;
    }

    /** Si la sesión de la página venció, Spring redirige al login y llega HTML en vez de JSON. */
    function esJson(respuesta) {
        const tipo = respuesta.headers.get('Content-Type') || '';
        return !respuesta.redirected && tipo.indexOf('application/json') !== -1;
    }

    function mensajeDeError(estado) {
        if (estado === 400) {
            return 'Escribe una pregunta de hasta 500 caracteres.';
        }
        if (estado === 401 || estado === 403 || estado === 0) {
            return 'Tu sesión terminó. Vuelve a iniciar sesión para seguir hablando conmigo.';
        }
        if (estado === 404) {
            return 'El asistente todavía no está disponible en este servidor.';
        }
        return 'Tuve un problema para responder. Intenta de nuevo en un momento.';
    }

    async function cargarHistorial() {
        if (!sesionId) {
            saludar();
            return;
        }
        try {
            const r = await fetch(URL_CHAT + '/' + encodeURIComponent(sesionId) + '/mensajes',
                {headers: {'Accept': 'application/json'}});
            if (!r.ok || !esJson(r)) {
                throw new Error('sin historial');
            }
            const lista = await r.json();
            if (!Array.isArray(lista) || lista.length === 0) {
                throw new Error('historial vacío');
            }
            lista.forEach(function (m) {
                agregarMensaje(m.texto, m.emisor);
            });
            mostrarSugerencias(SUGERENCIAS_INICIALES);
        } catch (e) {
            // La sesión ya no existe (o es de otro usuario): se empieza una nueva.
            sesionId = null;
            guardarSesion(null);
            saludar();
        }
    }

    async function enviar(texto) {
        texto = (texto || '').trim();
        if (!texto || enviando) {
            return;
        }
        enviando = true;
        campo.value = '';
        agregarMensaje(texto, 'USUARIO');
        mostrarSugerencias([]);
        const escribiendo = mostrarEscribiendo();

        try {
            const r = await fetch(URL_CHAT, {
                method: 'POST',
                headers: cabeceras(),
                body: JSON.stringify({sesionId: sesionId, mensaje: texto})
            });
            escribiendo.remove();
            if (!esJson(r)) {
                agregarMensaje(mensajeDeError(r.redirected ? 401 : r.status), 'BOT');
                return;
            }
            if (!r.ok) {
                agregarMensaje(mensajeDeError(r.status), 'BOT');
                mostrarSugerencias(SUGERENCIAS_INICIALES);
                return;
            }
            const datos = await r.json();
            sesionId = datos.sesionId || sesionId;
            guardarSesion(sesionId);
            agregarMensaje(datos.respuesta || '...', 'BOT');
            mostrarSugerencias(datos.sugerencias && datos.sugerencias.length ? datos.sugerencias : SUGERENCIAS_INICIALES);
        } catch (e) {
            escribiendo.remove();
            agregarMensaje('No pude conectarme con el servidor. Revisa tu conexión e intenta de nuevo.', 'BOT');
            mostrarSugerencias(SUGERENCIAS_INICIALES);
        } finally {
            enviando = false;
            campo.focus();
        }
    }

    // ---------- Abrir y cerrar ----------

    function abrir() {
        ventana.hidden = false;
        raiz.classList.add('abierto');
        boton.setAttribute('aria-expanded', 'true');
        boton.setAttribute('aria-label', 'Cerrar el asistente');
        if (!iniciado) {
            iniciado = true;
            cargarHistorial();
        }
        campo.focus();
    }

    function cerrar() {
        ventana.hidden = true;
        raiz.classList.remove('abierto');
        boton.setAttribute('aria-expanded', 'false');
        boton.setAttribute('aria-label', 'Abrir el asistente de TravelGo');
        boton.focus();
    }

    boton.addEventListener('click', function () {
        if (ventana.hidden) {
            abrir();
        } else {
            cerrar();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !ventana.hidden) {
            cerrar();
        }
    });

    formulario.addEventListener('submit', function (e) {
        e.preventDefault();
        enviar(campo.value);
    });

    nueva.addEventListener('click', function () {
        sesionId = null;
        guardarSesion(null);
        mensajes.replaceChildren();
        saludar();
        campo.focus();
    });
})();
