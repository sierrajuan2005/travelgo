// El backend calcula el total sumando los servicios reservados una sola vez.
(function () {
    const total = document.getElementById('total');
    const extras = document.getElementById('extras');
    if (!total || !extras) return;
    const opciones = document.querySelectorAll('input[data-precio]');
    function recalcular() {
        let suma = 0;
        opciones.forEach(radio => { if (radio.checked) suma += Number(radio.dataset.precio) || 0; });
        const texto = '$ ' + Math.round(suma).toLocaleString('es-CO');
        extras.textContent = texto;
        total.textContent = texto;
    }
    opciones.forEach(radio => radio.addEventListener('change', recalcular));
    recalcular();
})();
