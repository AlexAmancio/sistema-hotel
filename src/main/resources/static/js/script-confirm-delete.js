document.addEventListener("DOMContentLoaded", function () {
    const overlay = document.getElementById("confirm-modal-overlay");
    if (!overlay) return;

    const cancelar = document.getElementById("confirm-modal-cancelar");
    const confirmar = document.getElementById("confirm-modal-confirmar");
    const titulo = document.getElementById("confirm-modal-titulo");
    const texto = document.getElementById("confirm-modal-texto");
    const link = document.getElementById("confirm-modal-link");
    let formPendiente = null;

    document.querySelectorAll("form.form-eliminar").forEach(function (form) {
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            formPendiente = form;
            titulo.textContent = form.dataset.confirmTitulo || "¿Eliminar este elemento?";
            texto.textContent = form.dataset.confirmTexto || "Esta acción no se puede deshacer.";
            texto.classList.toggle("peligro", form.dataset.confirmPeligro === "true");
            confirmar.textContent = form.dataset.confirmBoton || "Eliminar";

            // Atajo opcional: por ejemplo, el check-out con saldo pendiente
            // ofrece ir a cobrar antes de seguir, en vez de solo avisar y
            // dejar que la plata se quede sin cobrar.
            if (form.dataset.confirmLinkHref) {
                link.href = form.dataset.confirmLinkHref;
                link.textContent = form.dataset.confirmLinkTexto || "Ir →";
                link.classList.remove("hidden");
            } else {
                link.classList.add("hidden");
            }

            overlay.classList.remove("hidden");
        });
    });

    cancelar.addEventListener("click", function () {
        formPendiente = null;
        overlay.classList.add("hidden");
    });

    overlay.addEventListener("click", function (e) {
        if (e.target === overlay) {
            formPendiente = null;
            overlay.classList.add("hidden");
        }
    });

    confirmar.addEventListener("click", function () {
        if (formPendiente) {
            formPendiente.submit();
        }
    });
});
