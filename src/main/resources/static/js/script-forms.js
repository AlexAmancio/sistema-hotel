document.addEventListener("DOMContentLoaded", function() {
    // Boton cancelar de los formularios
    let btnCloseModal = document.getElementById("btn-close");
    btnCloseModal.addEventListener("click", (e) => {
        e.preventDefault();
        window.history.back();
    });
});
