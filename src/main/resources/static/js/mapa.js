(function () {
    'use strict';

    const CATEGORIAS = [
        { id: 'RESTAURANTE', etiqueta: 'Restaurantes', color: '#e4572e' },
        { id: 'CAFE', etiqueta: 'Cafés', color: '#8d5524' },
        { id: 'HOTEL', etiqueta: 'Hoteles', color: '#1d6fa5' },
        { id: 'SITIO_TURISTICO', etiqueta: 'Sitios turísticos', color: '#2a9d8f' },
        { id: 'MUSEO', etiqueta: 'Museos', color: '#7b2cbf' }
    ];
    const seleccionadas = new Set();
    let ciudades = [];
    let marcadores = [];
    let avisos = [];

    const estado = document.getElementById('estado');
    const mapa = new maplibregl.Map({
        container: 'mapa',
        style: window.TRAVELGO_MAPA.estiloUrl,
        center: [-75.48, 10.39],
        zoom: 12
    });
    mapa.addControl(new maplibregl.NavigationControl(), 'top-right');
    mapa.addControl(new maplibregl.ScaleControl(), 'bottom-left');

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

    function crearFiltros() {
        const contenedor = document.getElementById('categorias');
        CATEGORIAS.forEach(function (c) {
            const boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'chip';
            boton.style.setProperty('--color', c.color);
            boton.textContent = c.etiqueta;
            boton.addEventListener('click', function () {
                if (seleccionadas.has(c.id)) { seleccionadas.delete(c.id); } else { seleccionadas.add(c.id); }
                boton.classList.toggle('activo', seleccionadas.has(c.id));
                aplicarFiltro();
            });
            contenedor.appendChild(boton);
        });
        document.getElementById('atenuar').addEventListener('change', aplicarFiltro);
    }

    // Sin categorías marcadas se ven todos; con alguna marcada, solo esas (los demás se ocultan o se atenúan).
    function aplicarFiltro() {
        const atenuar = document.getElementById('atenuar').checked;
        let visibles = 0;
        marcadores.forEach(function (m) {
            const visible = seleccionadas.size === 0 || seleccionadas.has(m.categoria);
            if (visible) { visibles++; }
            m.elemento.classList.toggle('oculto', !visible && !atenuar);
            m.elemento.classList.toggle('atenuado', !visible && atenuar);
        });
        estado.textContent = 'Mostrando ' + visibles + ' de ' + marcadores.length + ' lugares.'
            + (avisos.length ? ' ' + avisos.join(' · ') : '');
    }

    function limpiar() {
        marcadores.forEach(function (m) { m.marker.remove(); });
        marcadores = [];
        avisos = [];
    }

    function linea(texto) {
        const p = document.createElement('p');
        p.textContent = texto;
        return p;
    }

    function contenidoPopup(lugar) {
        const cat = categoriaDe(lugar.categoria);
        const caja = document.createElement('div');
        caja.className = 'popup';
        const titulo = document.createElement('h3');
        titulo.textContent = lugar.nombre;
        caja.appendChild(titulo);
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
            const url = '/api/hoteles-reales/' + encodeURIComponent(lugar.id) + '/tarifa?entrada='
                + fecha(entrada) + '&salida=' + fecha(salida);
            const oferta = await pedirJson(url);
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
        const elemento = document.createElement('div');
        elemento.className = 'pin';
        elemento.style.background = cat.color;
        const marker = new maplibregl.Marker({ element: elemento })
            .setLngLat([lugar.longitud, lugar.latitud])
            .setPopup(new maplibregl.Popup({ offset: 12 }).setDOMContent(contenidoPopup(lugar)))
            .addTo(mapa);
        marcadores.push({ categoria: lugar.categoria, marker: marker, elemento: elemento });
    }

    async function cargarCiudad(id) {
        const ciudad = ciudades.find(function (c) { return c.id === id; });
        mapa.flyTo({ center: [ciudad.longitud, ciudad.latitud], zoom: ciudad.zoom });
        limpiar();
        estado.textContent = 'Cargando lugares…';
        try {
            const datos = await pedirJson('/api/mapa/lugares?ciudad=' + encodeURIComponent(id));
            avisos = datos.avisos;
            datos.lugares.forEach(dibujar);
            aplicarFiltro();
        } catch (e) {
            estado.textContent = e.message;
        }
    }

    async function iniciar() {
        crearFiltros();
        try {
            ciudades = await pedirJson('/api/mapa/ciudades');
        } catch (e) {
            estado.textContent = e.message;
            return;
        }
        const selector = document.getElementById('ciudad');
        ciudades.forEach(function (c) {
            const opcion = document.createElement('option');
            opcion.value = c.id;
            opcion.textContent = c.nombre + ', ' + c.pais;
            selector.appendChild(opcion);
        });
        selector.addEventListener('change', function () { cargarCiudad(selector.value); });
        mapa.on('load', function () { cargarCiudad(selector.value); });
    }

    iniciar();
})();