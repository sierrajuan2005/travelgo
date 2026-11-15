// Recalcula el total de la reserva cuando el usuario elige vuelo, hotel o transporte.
// Cada radio tiene su precio en data-precio; el precio del paquete está en #total[data-base].
// Los precios están en pesos colombianos (COP), sin decimales: $ 1.200.000
(function () {
    const total = document.getElementById('total');
    const extras = document.getElementById('extras');
    const base = parseFloat(total.dataset.base);

    function formatear(valor) {
        return '$ ' + Math.round(valor).toLocaleString('es-CO', { maximumFractionDigits: 0 });
    }

    function recalcular() {
        let sumaExtras = 0;
        document.querySelectorAll('input[type="radio"]:checked').forEach(function (radio) {
            sumaExtras += parseFloat(radio.dataset.precio) || 0;
        });
        extras.textContent = formatear(sumaExtras);
        total.textContent = formatear(base + sumaExtras);
    }

    document.querySelectorAll('input[type="radio"]').forEach(function (radio) {
        radio.addEventListener('change', recalcular);
    });
})();
