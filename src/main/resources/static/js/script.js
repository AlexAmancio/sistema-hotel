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

});