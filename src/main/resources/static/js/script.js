document.addEventListener("DOMContentLoaded", function() {
    
    // Obtiene la ruta después del dominio
    let ruta = window.location.pathname;

    // Divide la ruta en un array utilizando "/"
    let elementosDeRuta = ruta.split("/");

    let elemento = null;

    // Muestra los elementos de la ruta en la consola
    switch (elementosDeRuta[1]) {
        case "":
            elemento = document.getElementById("item-registros");
            break;
        case "habitaciones":
            elemento = document.getElementById("item-habitaciones");
            break;
        case "ocupacion":
            elemento = document.getElementById("item-ocupacion");
            break;
        case "categorias":
            elemento = document.getElementById("item-categorias");
            break;
        case "clientes":
            elemento = document.getElementById("item-clientes");
            break;
        case "usuarios":
            elemento = document.getElementById("item-usuarios");
            break;
        case "hoy":
            elemento = document.getElementById("item-hoy");
            break;
        case "dashboard":
            elemento = document.getElementById("item-dashboard");
            break;
    }

    console.log(elementosDeRuta[1]);
    if (elemento) {
        elemento.classList.add("active");
    }

    // Evita doble submit (doble clic, doble Enter) en cualquier formulario normal:
    // deshabilita el botón apenas se envía. No interfiere con los formularios de
    // confirmación de borrado, que interceptan el submit con preventDefault() y
    // lo disparan después vía form.submit(), el cual no vuelve a emitir este evento.
    document.querySelectorAll("form:not(.form-eliminar)").forEach(function (form) {
        form.addEventListener("submit", function () {
            var boton = form.querySelector("button[type='submit']");
            if (boton && !boton.disabled) {
                boton.disabled = true;
            }
        });
    });

});