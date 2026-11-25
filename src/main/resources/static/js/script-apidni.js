const tipoDocumento = document.getElementById("tipoDocumento");
const identificacion = document.getElementById("identificacion");
const botonConsulta = document.getElementById("busqueda");
const estadoConsulta = document.getElementById("estado-consulta-dni");
const nombres = document.getElementById("nombres");
const apellidos = document.getElementById("apellidos");

let consultaActiva;

function mostrarEstado(texto, error = false) {
    estadoConsulta.classList.toggle("consulta-error", error);
    estadoConsulta.textContent = texto;
}

function actualizarModoDocumento() {
    consultaActiva?.abort();
    consultaActiva = null;

    const esDni = tipoDocumento.value === "DNI";
    botonConsulta.hidden = !esDni;
    botonConsulta.disabled = false;
    identificacion.inputMode = esDni ? "numeric" : "text";
    nombres.readOnly = esDni;
    apellidos.readOnly = esDni;
    mostrarEstado("");
}

async function consultarDni() {
    const dni = identificacion.value.trim();
    if (!/^\d{8}$/.test(dni)) {
        mostrarEstado("Ingresa un DNI de 8 dígitos.", true);
        identificacion.focus();
        return;
    }

    const consulta = new AbortController();
    consultaActiva = consulta;
    botonConsulta.disabled = true;
    mostrarEstado("Consultando DNI…");

    try {
        const respuesta = await fetch("/clientes/dni/" + dni, { signal: consulta.signal });
        if (!respuesta.ok) throw new Error("Consulta fallida");

        const { data } = await respuesta.json();
        const apellidosDni = [data?.apellido_paterno, data?.apellido_materno].filter(Boolean).join(" ");
        if (!data?.nombres || !apellidosDni) throw new Error("Respuesta incompleta");
        if (consultaActiva !== consulta) return;

        nombres.value = data.nombres;
        apellidos.value = apellidosDni;
        nombres.readOnly = true;
        apellidos.readOnly = true;
        mostrarEstado("Datos del DNI encontrados.");
    } catch {
        if (consultaActiva !== consulta) return;

        nombres.readOnly = false;
        apellidos.readOnly = false;
        mostrarEstado("No se pudo consultar el DNI. Puedes ingresar los nombres y apellidos manualmente.", true);
    } finally {
        if (consultaActiva === consulta) {
            consultaActiva = null;
            botonConsulta.disabled = false;
        }
    }
}

tipoDocumento.addEventListener("change", actualizarModoDocumento);
identificacion.addEventListener("input", actualizarModoDocumento);
botonConsulta.addEventListener("click", consultarDni);
actualizarModoDocumento();
