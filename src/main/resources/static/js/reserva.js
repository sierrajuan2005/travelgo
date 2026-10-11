// Recalcula el total de la reserva cuando el usuario elige vuelo, hotel o transporte.
// Cada radio tiene su precio en data-precio; el precio del paquete está en #total[data-base].
(function () {
    const total = document.getElementById('total');
    const extras = document.getElementById('extras');
    const base = parseFloat(total.dataset.base);

    function formatear(valor) {
        return 'USD ' + valor.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
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
