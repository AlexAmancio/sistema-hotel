var boton = document.getElementById("busqueda");
var selectTipoDocumento = document.getElementById("tipoDocumento");
var campoNombres = document.getElementById("nombres");
var campoApellidos = document.getElementById("apellidos");

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

// La API de búsqueda solo conoce DNI (consulta RENIEC). Con pasaporte o
// carné de extranjería no hay forma de autocompletar: se habilita la carga
// manual de nombres y apellidos y se oculta el botón de búsqueda.
function actualizarModoDocumento() {
    var esDni = selectTipoDocumento.value === "DNI";
    boton.classList.toggle("hidden", !esDni);
    campoNombres.readOnly = esDni;
    campoApellidos.readOnly = esDni;
}

if (selectTipoDocumento) {
    selectTipoDocumento.addEventListener("change", actualizarModoDocumento);
    actualizarModoDocumento();
}