function mostrarToast(mensaje, tipo) {
    if (!mensaje) return;

    var contenedor = document.getElementById('toast-container');
    if (!contenedor) return;

    var toast = document.createElement('div');
    toast.className = 'toast' + (tipo === 'error' ? ' toast-error' : '');

    var icono = document.createElement('div');
    icono.className = 'toast-icono';
    icono.innerHTML = tipo === 'error'
        ? '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="9" stroke="#E5484D" stroke-width="1.6"/><path d="M10 6V11" stroke="#E5484D" stroke-width="1.6" stroke-linecap="round"/><circle cx="10" cy="13.6" r="1" fill="#E5484D"/></svg>'
        : '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="9" stroke="#0EA968" stroke-width="1.6"/><path d="M6 10.2L8.6 12.8L14 7.2" stroke="#0EA968" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';

    var texto = document.createElement('div');
    texto.className = 'toast-mensaje';
    texto.textContent = mensaje;

    var cerrar = document.createElement('button');
    cerrar.className = 'toast-cerrar';
    cerrar.setAttribute('aria-label', 'Cerrar');
    cerrar.innerHTML = '&times;';

    function quitar() {
        toast.classList.add('toast-saliendo');
        setTimeout(function () { toast.remove(); }, 250);
    }
    cerrar.addEventListener('click', quitar);

    toast.appendChild(icono);
    toast.appendChild(texto);
    toast.appendChild(cerrar);
    contenedor.appendChild(toast);

    setTimeout(quitar, 4000);
}
