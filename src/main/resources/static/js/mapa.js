(function () {
    'use strict';

    const CATEGORIAS = [
        { id: 'RESTAURANTE', etiqueta: 'Restaurantes', color: '#e4572e' },
        { id: 'CAFE', etiqueta: 'Cafés', color: '#8d5524' },
        { id: 'HOTEL', etiqueta: 'Hoteles', color: '#1d6fa5' },
        { id: 'SITIO_TURISTICO', etiqueta: 'Sitios turísticos', color: '#2a9d8f' },
        { id: 'MUSEO', etiqueta: 'Museos', color: '#7b2cbf' }
    ];

    function normalizar(texto) {
        return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
    }

    async function pedirJson(url) {
        const respuesta = await fetch(url, { headers: { 'Accept': 'application/json' } });
        const tipo = respuesta.headers.get('content-type') || '';
        if (!tipo.includes('json')) {
            throw new Error('Inicia sesión para usar el mapa.');
        }
        const datos = await respuesta.json();
        if (!respuesta.ok) {
            throw new Error(datos.error || 'No se pudo completar la consulta.');
        }
        return datos;
    }

    function categoriaDe(id) {
        return CATEGORIAS.find(function (c) { return c.id === id; });
    }

    function linea(texto, clase) {
        const p = document.createElement('p');
        p.textContent = texto;
        if (clase) { p.className = clase; }
        return p;
    }

    // El mapa se crea solo cuando la tarjeta se acerca a la pantalla (ahorra teselas).
    function cuandoSeVea(elemento, accion) {
        if (!('IntersectionObserver' in window)) { accion(); return; }
        const observador = new IntersectionObserver(function (entradas) {
            if (entradas.some(function (e) { return e.isIntersecting; })) {
                observador.disconnect();
                accion();
            }
        }, { rootMargin: '200px' });
        observador.observe(elemento);
    }

    function montar(raiz) {
        const lienzo = raiz.querySelector('[data-tgm="lienzo"]');
        const filtros = raiz.querySelector('[data-tgm="filtros"]');
        const opcion = raiz.querySelector('[data-tgm="opcion"]');
        const atenuar = raiz.querySelector('[data-tgm="atenuar"]');
        const estado = raiz.querySelector('[data-tgm="estado"]');
        const selector = raiz.querySelector('[data-tgm="ciudad"]');
        const ciudadFija = raiz.dataset.ciudad || '';
        const paqueteId = raiz.dataset.paquete || '';

        const seleccionadas = new Set();
        let ciudades = [];
        let ciudadActual = null;
        let mapa = null;
        let marcadores = [];
        let avisos = [];
        let hotelClave = null;

        function crearFiltros() {
            CATEGORIAS.forEach(function (c) {
                const boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'tgm-chip';
                boton.style.setProperty('--color', c.color);
                boton.textContent = c.etiqueta;
                boton.addEventListener('click', function () {
                    if (seleccionadas.has(c.id)) { seleccionadas.delete(c.id); } else { seleccionadas.add(c.id); }
                    boton.classList.toggle('activo', seleccionadas.has(c.id));
                    aplicarFiltro();
                });
                filtros.appendChild(boton);
            });
            atenuar.addEventListener('change', aplicarFiltro);
        }

        // Sin categorías marcadas se ven todos; con alguna marcada, solo esas. El hotel del paquete siempre se ve.
        function aplicarFiltro() {
            let visibles = 0;
            marcadores.forEach(function (m) {
                const visible = m.destacado || seleccionadas.size === 0 || seleccionadas.has(m.categoria);
                if (visible) { visibles++; }
                m.elemento.classList.toggle('tgm-oculto', !visible && !atenuar.checked);
                m.elemento.classList.toggle('tgm-atenuado', !visible && atenuar.checked);
            });
            estado.textContent = 'Mostrando ' + visibles + ' de ' + marcadores.length + ' lugares.'
                + (avisos.length ? ' ' + avisos.join(' · ') : '');
        }

        function contenidoPopup(lugar, destacado) {
            const cat = categoriaDe(lugar.categoria);
            const caja = document.createElement('div');
            caja.className = 'tgm-popup';
            const titulo = document.createElement('h3');
            titulo.textContent = lugar.nombre;
            caja.appendChild(titulo);
            if (destacado) { caja.appendChild(linea('Hotel de este paquete', 'tgm-tu-hotel')); }
            caja.appendChild(linea(cat.etiqueta));
            if (lugar.direccion) { caja.appendChild(linea(lugar.direccion)); }
            if (lugar.calificacion) { caja.appendChild(linea('Calificación: ' + lugar.calificacion + ' / 5')); }
            if (lugar.enlace) {
                const enlace = document.createElement('a');
                enlace.href = lugar.enlace;
                enlace.target = '_blank';
                enlace.rel = 'noopener';
                enlace.textContent = 'Ver más información';
                caja.appendChild(enlace);
            }
            if (lugar.categoria === 'HOTEL') {
                const resultado = linea('');
                const boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'btn btn-sm btn-outline-secondary';
                boton.textContent = 'Ver precio de referencia';
                boton.addEventListener('click', function () { mostrarPrecio(lugar, boton, resultado); });
                caja.appendChild(boton);
                caja.appendChild(resultado);
            }
            return caja;
        }

        // Precio de referencia de Xotelo: 3 noches empezando dentro de 30 días.
        async function mostrarPrecio(lugar, boton, resultado) {
            const dia = 86400000;
            const entrada = new Date(Date.now() + 30 * dia);
            const salida = new Date(entrada.getTime() + 3 * dia);
            const fecha = function (d) { return d.toISOString().slice(0, 10); };
            boton.disabled = true;
            resultado.textContent = 'Consultando…';
            try {
                const oferta = await pedirJson('/api/hoteles-reales/' + encodeURIComponent(lugar.id)
                    + '/tarifa?entrada=' + fecha(entrada) + '&salida=' + fecha(salida));
                const consulta = new Date(oferta.fechaConsulta).toLocaleDateString('es-CO');
                resultado.textContent = 'Desde ' + oferta.mejorTarifa.precioPorNoche + ' ' + oferta.moneda
                    + ' por noche en ' + oferta.mejorTarifa.agencia + ' (' + oferta.noches + ' noches, del '
                    + fecha(entrada) + '). Precio de referencia consultado el ' + consulta + '; puede cambiar.';
            } catch (e) {
                resultado.textContent = e.message;
            }
            boton.disabled = false;
        }

        function dibujar(lugar) {
            const cat = categoriaDe(lugar.categoria);
            const destacado = lugar.categoria === 'HOTEL' && hotelClave !== null && lugar.id === hotelClave;
            const elemento = document.createElement('div');
            elemento.className = 'tgm-pin' + (destacado ? ' tgm-destacado' : '');
            elemento.style.background = cat.color;
            const marker = new maplibregl.Marker({ element: elemento })
                .setLngLat([lugar.longitud, lugar.latitud])
                .setPopup(new maplibregl.Popup({ offset: 14 }).setDOMContent(contenidoPopup(lugar, destacado)))
                .addTo(mapa);
            marcadores.push({ categoria: lugar.categoria, marker: marker, elemento: elemento, destacado: destacado });
        }

        function limpiar() {
            marcadores.forEach(function (m) { m.marker.remove(); });
            marcadores = [];
            avisos = [];
        }

        async function cargarCiudad(ciudad) {
            ciudadActual = ciudad;
            mapa.flyTo({ center: [ciudad.longitud, ciudad.latitud], zoom: ciudad.zoom });
            limpiar();
            estado.textContent = 'Cargando lugares…';
            try {
                const datos = await pedirJson('/api/mapa/lugares?ciudad=' + encodeURIComponent(ciudad.id));
                avisos = datos.avisos;
                datos.lugares.forEach(dibujar);
                const hotelDelPaquete = marcadores.find(function (m) { return m.destacado; });
                if (hotelDelPaquete) {
                    mapa.flyTo({ center: hotelDelPaquete.marker.getLngLat(), zoom: 14 });
                    hotelDelPaquete.marker.togglePopup();
                } else if (hotelClave) {
                    avisos.push('El hotel de este paquete no aparece en el mapa.');
                }
                aplicarFiltro();
            } catch (e) {
                estado.textContent = e.message;
            }
        }

        function crearMapa() {
            mapa = new maplibregl.Map({
                container: lienzo,
                style: raiz.dataset.estilo,
                center: [ciudadActual.longitud, ciudadActual.latitud],
                zoom: ciudadActual.zoom
            });
            mapa.addControl(new maplibregl.NavigationControl(), 'top-right');
            mapa.addControl(new maplibregl.ScaleControl(), 'bottom-left');
            mapa.on('load', function () { cargarCiudad(ciudadActual); });
        }

        async function descubrirHotelDelPaquete() {
            if (!paqueteId) { return; }
            try {
                const paquete = await pedirJson('/api/paquetes-reales/' + encodeURIComponent(paqueteId));
                const hotel = paquete.servicios.find(function (s) { return s.tipo === 'Hotel' && s.idExterno; });
                if (hotel) { hotelClave = hotel.idExterno; }
            } catch (e) {
                // Sin sesión o paquete manual: el mapa funciona igual, solo sin resaltar el hotel.
            }
        }

        async function iniciar() {
            crearFiltros();
            try {
                ciudades = await pedirJson('/api/mapa/ciudades');
            } catch (e) {
                estado.textContent = e.message;
                if (ciudadFija) {
                    // En la ficha del paquete: solo avisa, sin romper la página.
                    [lienzo, filtros, opcion].forEach(function (n) { n.classList.add('d-none'); });
                    raiz.classList.remove('d-none');
                }
                return;
            }
            if (ciudadFija) {
                ciudadActual = ciudades.find(function (c) { return normalizar(c.nombre) === normalizar(ciudadFija); });
                if (!ciudadActual) { return; } // ciudad sin mapa: la tarjeta sigue oculta
            } else {
                ciudades.forEach(function (c) {
                    const o = document.createElement('option');
                    o.value = c.id;
                    o.textContent = c.nombre + ', ' + c.pais;
                    selector.appendChild(o);
                });
                ciudadActual = ciudades[0];
                selector.addEventListener('change', function () {
                    cargarCiudad(ciudades.find(function (c) { return c.id === selector.value; }));
                });
            }
            raiz.classList.remove('d-none');
            cuandoSeVea(raiz, async function () {
                await descubrirHotelDelPaquete();
                crearMapa();
            });
        }

        iniciar();
    }

    function arrancar() {
        document.querySelectorAll('[data-tgm-raiz]').forEach(montar);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', arrancar);
    } else {
        arrancar();
    }
})();