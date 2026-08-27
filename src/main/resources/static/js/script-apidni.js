var boton = document.getElementById("busqueda");

function traerdatos(){
    var dni = document.getElementById("identificacion").value;
    fetch("/clientes/dni/" + dni)
        .then((res) => res.json())
        .then((data) => {
            document.getElementById("nombres").value = data.data.nombres;
            document.getElementById("apellidos").value = data.data.apellido_paterno + " " + data.data.apellido_materno;
        })
        .catch((error) => {
            console.error('Error al obtener datos:', error);
        });
}
boton.addEventListener("click", traerdatos);