(function () {
    'use strict';

    const CATEGORIAS = [
        {
            "id": "RESTAURANTE",
            "etiqueta": "Restaurantes",
            "color": "#e4572e",
            "icono": "🍴",
            "grupo": "Gastronomía"
        },
        {
            "id": "CAFE",
            "etiqueta": "Cafés",
            "color": "#8d5524",
            "icono": "☕",
            "grupo": "Gastronomía"
        },
        {
            "id": "COMIDA_RAPIDA",
            "etiqueta": "Comida rápida",
            "color": "#d97706",
            "icono": "🍔",
            "grupo": "Gastronomía"
        },
        {
            "id": "BAR",
            "etiqueta": "Bares",
            "color": "#be185d",
            "icono": "🍸",
            "grupo": "Gastronomía"
        },
        {
            "id": "PANADERIA",
            "etiqueta": "Panaderías",
            "color": "#a16207",
            "icono": "🥐",
            "grupo": "Gastronomía"
        },
        {
            "id": "HELADERIA",
            "etiqueta": "Heladerías",
            "color": "#db2777",
            "icono": "🍦",
            "grupo": "Gastronomía"
        },
        {
            "id": "HOTEL",
            "etiqueta": "Hoteles",
            "color": "#1d6fa5",
            "icono": "🛏",
            "grupo": "Alojamiento y compras"
        },
        {
            "id": "HOSTAL",
            "etiqueta": "Hostales",
            "color": "#0369a1",
            "icono": "🏠",
            "grupo": "Alojamiento y compras"
        },
        {
            "id": "CENTRO_COMERCIAL",
            "etiqueta": "Centros comerciales",
            "color": "#c026d3",
            "icono": "🛍",
            "grupo": "Alojamiento y compras"
        },
        {
            "id": "SUPERMERCADO",
            "etiqueta": "Supermercados",
            "color": "#ea580c",
            "icono": "🛒",
            "grupo": "Alojamiento y compras"
        },
        {
            "id": "MERCADO",
            "etiqueta": "Mercados locales",
            "color": "#64748b",
            "icono": "🧺",
            "grupo": "Alojamiento y compras"
        },
        {
            "id": "SITIO_TURISTICO",
            "etiqueta": "Sitios turísticos",
            "color": "#2a9d8f",
            "icono": "📷",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "MUSEO",
            "etiqueta": "Museos",
            "color": "#7b2cbf",
            "icono": "🏛",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "MONUMENTO",
            "etiqueta": "Monumentos",
            "color": "#475569",
            "icono": "🗿",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "MIRADOR",
            "etiqueta": "Miradores",
            "color": "#15803d",
            "icono": "🔭",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "PARQUE",
            "etiqueta": "Parques",
            "color": "#16a34a",
            "icono": "🌳",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "PLAYA",
            "etiqueta": "Playas",
            "color": "#0891b2",
            "icono": "🏖",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "IGLESIA",
            "etiqueta": "Iglesias",
            "color": "#92400e",
            "icono": "⛪",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "HISTORICO",
            "etiqueta": "Sitios históricos",
            "color": "#475569",
            "icono": "🏰",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "PARQUE_TEMATICO",
            "etiqueta": "Parques temáticos",
            "color": "#9333ea",
            "icono": "🎢",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "TEATRO",
            "etiqueta": "Teatros",
            "color": "#be123c",
            "icono": "🎭",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "CINE",
            "etiqueta": "Cines",
            "color": "#4338ca",
            "icono": "🎬",
            "grupo": "Turismo y entretenimiento"
        },
        {
            "id": "AEROPUERTO",
            "etiqueta": "Aeropuertos",
            "color": "#2563eb",
            "icono": "✈",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "TERMINAL",
            "etiqueta": "Terminales",
            "color": "#ca8a04",
            "icono": "🚌",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "TREN",
            "etiqueta": "Estaciones de tren",
            "color": "#475569",
            "icono": "🚆",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "HOSPITAL",
            "etiqueta": "Hospitales",
            "color": "#dc2626",
            "icono": "🏥",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "FARMACIA",
            "etiqueta": "Farmacias",
            "color": "#059669",
            "icono": "💊",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "CAJERO",
            "etiqueta": "Cajeros automáticos",
            "color": "#15803d",
            "icono": "💵",
            "grupo": "Transporte y servicios"
        },
        {
            "id": "POLICIA",
            "etiqueta": "Estaciones de policía",
            "color": "#1e40af",
            "icono": "👮",
            "grupo": "Transporte y servicios"
        }
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
        const destinoId = raiz.dataset.destino || '';
        const panel = raiz.querySelector('[data-tgm="panel"]');
        const abrir = raiz.querySelector('[data-tgm="abrir"]');
        abrir.addEventListener('click', () => { panel.hidden = !panel.hidden; abrir.setAttribute('aria-expanded', String(!panel.hidden)); });

        const seleccionadas = new Set(['SITIO_TURISTICO']);
        const radio = raiz.querySelector('[data-tgm="radio"]');
        const limite = raiz.querySelector('[data-tgm="limite"]');
        const buscar = raiz.querySelector('[data-tgm="buscar"]');
        const cargadas = new Set();
        const pendientes = new Set();
        let generacion = 0;
        let grupos = [];
        let ciudades = [];
        let ciudadActual = null;
        let mapa = null;
        let marcadores = [];
        let avisos = [];
        let hotelClave = null;

        function crearFiltros() {
            if (paqueteId) seleccionadas.add('HOTEL');
            const secciones = new Map();
            CATEGORIAS.forEach(c => {
                if (!secciones.has(c.grupo)) {
                    const seccion = document.createElement('details');
                    seccion.open = false;
                    const titulo = document.createElement('summary'); titulo.textContent = c.grupo;
                    seccion.appendChild(titulo); filtros.appendChild(seccion); secciones.set(c.grupo,seccion);
                }
                const boton = document.createElement('button');
                boton.type = 'button'; boton.className = 'tgm-chip';
                boton.style.setProperty('--color', c.color); boton.textContent = c.icono + ' ' + c.etiqueta;
                boton.classList.toggle('activo', seleccionadas.has(c.id));
                boton.setAttribute('aria-pressed', seleccionadas.has(c.id));
                boton.addEventListener('click', () => {
                    if (seleccionadas.has(c.id)) seleccionadas.delete(c.id); else seleccionadas.add(c.id);
                    boton.classList.toggle('activo', seleccionadas.has(c.id));
                    boton.setAttribute('aria-pressed', seleccionadas.has(c.id));
                    if (mapa && ciudadActual) cargarCategoria(c.id);
                    aplicarFiltro();
                });
                secciones.get(c.grupo).appendChild(boton);
                if (['RESTAURANTE','CAFE','HOTEL','SITIO_TURISTICO','MUSEO'].includes(c.id)) {
                    const rapido = boton.cloneNode(true);
                    rapido.addEventListener('click', () => boton.click());
                    boton.addEventListener('click', () => {
                        rapido.classList.toggle('activo', seleccionadas.has(c.id));
                        rapido.setAttribute('aria-pressed', String(seleccionadas.has(c.id)));
                    });
                    raiz.querySelector('[data-tgm="rapidos"]').appendChild(rapido);
                }
            });
            atenuar.addEventListener('change', aplicarFiltro);
            buscar.addEventListener('input', aplicarFiltro);
            [radio, limite].forEach(n => n.addEventListener('change', () => { if (mapa && ciudadActual) cargarCiudad(ciudadActual); }));
        }

        function distancia(l) {
            const rad = x => x * Math.PI / 180;
            const a = Math.sin(rad(l.latitud-ciudadActual.latitud)/2)**2 + Math.cos(rad(ciudadActual.latitud))*Math.cos(rad(l.latitud))*Math.sin(rad(l.longitud-ciudadActual.longitud)/2)**2;
            return 12742 * Math.asin(Math.sqrt(Math.min(1,a)));
        }
        function aplicarFiltro() {
            if (!mapa) return;
            grupos.forEach(g => g.remove()); grupos = [];
            const celdas = new Map(); let visibles = 0; let hoteles = 0;
            const ordenados = [...marcadores].sort((a,b) => distancia(a.lugar)-distancia(b.lugar));
            ordenados.forEach(m => {
                const coincide = normalizar(m.lugar.nombre).includes(normalizar(buscar.value));
                const activo = [...m.categorias].some(c => seleccionadas.has(c));
                const dentro = distancia(m.lugar) <= Number(radio.value)/1000;
                const permitido = m.categoria !== 'HOTEL' || (dentro && hoteles++ < Number(limite.value));
                const visible = m.destacado || (activo && coincide && permitido);
                m.elemento.classList.toggle('tgm-oculto', !visible && !(atenuar.checked && coincide && permitido));
                m.elemento.classList.toggle('tgm-atenuado', !visible && atenuar.checked);
                if (!visible) return;
                visibles++;
                if (m.destacado || mapa.getZoom() >= 17) return;
                const p = mapa.project(m.marker.getLngLat());
                const k = Math.floor(p.x/55)+':'+Math.floor(p.y/55);
                if (!celdas.has(k)) celdas.set(k,[]); celdas.get(k).push(m);
            });
            celdas.forEach(ms => {
                if (ms.length < 2) return;
                ms.forEach(m => m.elemento.classList.add('tgm-oculto'));
                const b = document.createElement('button'); b.type='button'; b.className='tgm-cluster'; b.textContent=ms.length;
                b.setAttribute('aria-label', ms.length+' lugares; acercar');
                const centro = [ms.reduce((v,m)=>v+m.lugar.longitud,0)/ms.length,ms.reduce((v,m)=>v+m.lugar.latitud,0)/ms.length];
                b.addEventListener('click',()=>mapa.easeTo({center:centro,zoom:Math.min(17,mapa.getZoom()+2)}));
                grupos.push(new maplibregl.Marker({element:b}).setLngLat(centro).addTo(mapa));
            });
            estado.textContent = 'Mostrando '+visibles+' lugares de las categorías seleccionadas.'+(pendientes.size?' Cargando…':'')+(avisos.length?' '+avisos.join(' · '):'');
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
            caja.appendChild(linea(distancia(lugar).toFixed(2) + " km del centro (línea recta)"));
            if (lugar.direccion) { caja.appendChild(linea(lugar.direccion)); }
            if (lugar.calificacion) { caja.appendChild(linea('Calificación: ' + lugar.calificacion + ' / 5')); }
            if (lugar.enlace && /^https?:\/\//i.test(lugar.enlace)) {
                const enlace = document.createElement('a');
                enlace.href = lugar.enlace;
                enlace.target = '_blank';
                enlace.rel = 'noopener';
                enlace.textContent = 'Ver más información';
                caja.appendChild(enlace);
            }
            if (lugar.categoria === 'HOTEL' && lugar.fuente === 'Xotelo (TripAdvisor)') {
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

        function dibujar(lugar, solicitada) {
            const existente = marcadores.find(m => m.lugar.id === lugar.id && m.lugar.fuente === lugar.fuente);
            if (existente) {
                existente.categorias.add(solicitada); existente.categorias.add(lugar.categoria);
                if (existente.categoria === 'SITIO_TURISTICO' && lugar.categoria !== 'SITIO_TURISTICO') {
                    const c = categoriaDe(lugar.categoria); existente.categoria = lugar.categoria; existente.lugar = lugar;
                    existente.elemento.style.background = c.color; existente.elemento.textContent = c.icono;
                    existente.marker.setPopup(new maplibregl.Popup({offset:20}).setDOMContent(contenidoPopup(lugar,false)));
                }
                return;
            }
            const cat = categoriaDe(lugar.categoria);
            const destacado = lugar.categoria === 'HOTEL' && hotelClave !== null && lugar.id === hotelClave;
            const elemento = document.createElement('div');
            elemento.className = 'tgm-pin' + (destacado ? ' tgm-destacado' : '');
            elemento.style.background = cat.color;
            elemento.textContent = cat.icono;
            elemento.setAttribute('aria-label', cat.etiqueta + ': ' + lugar.nombre);
            elemento.title = lugar.nombre;
            const marker = new maplibregl.Marker({ element: elemento })
                .setLngLat([lugar.longitud, lugar.latitud])
                .setPopup(new maplibregl.Popup({ offset: 20 }).setDOMContent(contenidoPopup(lugar, destacado)))
                .addTo(mapa);
            marcadores.push({ lugar, categorias: new Set([solicitada,lugar.categoria]), categoria: lugar.categoria, marker: marker, elemento: elemento, destacado: destacado });
        }

        function limpiar() {
            generacion++; cargadas.clear(); pendientes.clear();
            marcadores.forEach(m => m.marker.remove()); grupos.forEach(g => g.remove());
            marcadores = []; grupos = []; avisos = [];
        }
        async function cargarCategoria(id) {
            if ((!seleccionadas.has(id) && !(id === 'HOTEL' && hotelClave)) || cargadas.has(id) || pendientes.has(id)) return;
            const version = generacion; pendientes.add(id); aplicarFiltro();
            try {
                const datos = await pedirJson('/api/mapa/lugares?ciudad='+encodeURIComponent(ciudadActual.id)+'&categorias='+id+'&radio='+radio.value+'&limite='+limite.value);
                if (version !== generacion) return;
                avisos.push(...datos.avisos); datos.lugares.forEach(l=>dibujar(l,id));
                if (!datos.avisos.length) cargadas.add(id);
            } catch(e) { if (version === generacion) avisos.push(e.message); }
            finally { if (version === generacion) { pendientes.delete(id); aplicarFiltro(); } }
        }
        async function cargarCiudad(ciudad) {
            ciudadActual = ciudad; limpiar(); const version = generacion;
            mapa.flyTo({center:[ciudad.longitud,ciudad.latitud],zoom:ciudad.zoom});
            await Promise.all([...new Set([...seleccionadas,...(hotelClave?['HOTEL']:[])])].map(cargarCategoria));
            if (version !== generacion) return;
            const hotel = marcadores.find(m=>m.destacado);
            if (hotel) { mapa.flyTo({center:hotel.marker.getLngLat(),zoom:14}); hotel.marker.togglePopup(); }
            else if (hotelClave) avisos.push('El hotel de este paquete no aparece en el mapa.');
            aplicarFiltro();
        }

        function crearMapa() {
            try {
                mapa = new maplibregl.Map({
                    container: lienzo,
                    style: raiz.dataset.estilo || 'https://tiles.openfreemap.org/styles/liberty',
                    center: [ciudadActual.longitud, ciudadActual.latitud],
                    zoom: ciudadActual.zoom
                });
            } catch (e) {
                estado.textContent = 'No se pudo iniciar el mapa: ' + e.message;
                return;
            }
            mapa.addControl(new maplibregl.NavigationControl(), 'top-right');
            mapa.addControl(new maplibregl.ScaleControl(), 'bottom-left');
            if (maplibregl.FullscreenControl) mapa.addControl(new maplibregl.FullscreenControl(), 'top-right');
            mapa.on('error', function (evento) {
                const mensaje = evento && evento.error && evento.error.message ? evento.error.message : 'error desconocido';
                estado.textContent = 'Problema con el mapa: ' + mensaje;
            });
            mapa.on('moveend', aplicarFiltro);
            mapa.on('load', function () {
                mapa.resize();
                cargarCiudad(ciudadActual);
            });
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
                ciudades = destinoId ? [await pedirJson('/api/mapa/ciudad?id=destino-'+encodeURIComponent(destinoId))] : await pedirJson('/api/mapa/ciudades');
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
                if (!ciudadActual) { raiz.classList.remove('d-none'); lienzo.hidden = true; estado.textContent = 'No se pudo localizar este destino.'; return; }
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