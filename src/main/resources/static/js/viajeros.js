// Viajeros y gustos de una reserva (reserva/viajeros.html).
// Agrega y quita viajeros, muestra las actividades que van con los gustos de cada uno y suma el total.
(function () {
    const form = document.getElementById('form-viajeros');
    if (!form) return;

    const lista = document.getElementById('lista-viajeros');
    const molde = document.getElementById('molde-viajero');
    const botonAgregar = document.getElementById('agregar-viajero');
    const maximo = Number(form.dataset.maximo) || 8;
    const base = Number(form.dataset.base) || 0;

    // Mismo formato que FormatoWeb.cop: "$ 1.234.567"
    const cop = valor => '$ ' + Math.round(valor).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');

    const tarjetas = () => Array.from(lista.querySelectorAll('.tg-viajero'));

    /** Muestra solo los grupos de actividades de los gustos marcados (o todos con "Ver todas"). */
    function filtrar(tarjeta) {
        const todas = tarjeta.dataset.todas === '1';
        const gustos = new Set(Array.from(tarjeta.querySelectorAll('.tg-gusto-check:checked')).map(c => c.value));
        let visibles = 0;
        tarjeta.querySelectorAll('.tg-grupo-actividades').forEach(grupo => {
            const algunaMarcada = grupo.querySelector('.tg-actividad-check:checked') !== null;
            const ver = todas || gustos.has(grupo.dataset.gusto) || algunaMarcada;
            grupo.hidden = !ver;
            if (ver) visibles++;
        });
        const pista = tarjeta.querySelector('.tg-sin-sugerencias');
        if (pista) {
            pista.hidden = visibles > 0;
            if (gustos.size > 0 && visibles === 0) {
                pista.textContent = 'No hay actividades pagas para esos gustos; igual le armamos el plan con lugares reales.';
                pista.hidden = false;
            }
        }
        const boton = tarjeta.querySelector('.tg-ver-todas');
        if (boton) boton.textContent = todas ? 'Solo sus gustos' : 'Ver todas';
    }

    function totales() {
        const marcadas = Array.from(lista.querySelectorAll('.tg-actividad-check:checked'));
        const suma = marcadas.reduce((total, c) => total + (Number(c.dataset.precio) || 0), 0);
        document.getElementById('cuenta-actividades').textContent = marcadas.length;
        document.getElementById('total-actividades').textContent = cop(suma);
        document.getElementById('total-viaje').textContent = cop(base + suma);
        botonAgregar.disabled = tarjetas().length >= maximo;
        const solo = tarjetas().length === 1;
        tarjetas().forEach(t => t.querySelector('.tg-quitar-viajero').hidden = solo);
    }

    /** Pone los índices 0, 1, 2... en los nombres de los campos (viajeros[0].nombre ...). */
    function renumerar() {
        tarjetas().forEach((tarjeta, i) => {
            tarjeta.dataset.indice = i;
            tarjeta.querySelectorAll('[name]').forEach(campo => {
                campo.name = campo.name.replace(/viajeros\[(\d+|NUEVO)\]/, 'viajeros[' + i + ']');
            });
            tarjeta.querySelectorAll('[id^="nombre-"]').forEach(campo => campo.id = 'nombre-' + i);
            tarjeta.querySelectorAll('label[for^="nombre-"]').forEach(label => label.htmlFor = 'nombre-' + i);
        });
    }

    botonAgregar.addEventListener('click', () => {
        if (tarjetas().length >= maximo) return;
        const nueva = molde.content.firstElementChild.cloneNode(true);
        lista.appendChild(nueva);
        renumerar();
        filtrar(nueva);
        totales();
        nueva.querySelector('.tg-viajero-nombre').focus();
    });

    lista.addEventListener('click', evento => {
        const tarjeta = evento.target.closest('.tg-viajero');
        if (!tarjeta) return;
        if (evento.target.closest('.tg-quitar-viajero') && tarjetas().length > 1) {
            tarjeta.remove();
            renumerar();
            totales();
        }
        if (evento.target.closest('.tg-ver-todas')) {
            tarjeta.dataset.todas = tarjeta.dataset.todas === '1' ? '0' : '1';
            filtrar(tarjeta);
        }
    });

    lista.addEventListener('change', evento => {
        const tarjeta = evento.target.closest('.tg-viajero');
        if (!tarjeta) return;
        if (evento.target.classList.contains('tg-gusto-check')) filtrar(tarjeta);
        totales();
    });

    form.addEventListener('submit', renumerar);

    tarjetas().forEach(filtrar);
    totales();
})();
