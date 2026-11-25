document.addEventListener("DOMContentLoaded", function() {

    // Abrir submenu de opciones
    const iconSubmenuList = document.querySelectorAll(".icon-three");

    let actualIconSubmenu = null;

    function cerrarSubmenu() {
        if (!actualIconSubmenu) return;
        actualIconSubmenu.nextElementSibling.classList.remove("active");
        actualIconSubmenu.setAttribute("aria-expanded", "false");
        actualIconSubmenu = null;
    }

    // Agregar un event listener para detectar clics en el documento
    document.addEventListener("click", (e) => {
        // Verificar si el clic no se produjo dentro de algún ícono o submenu
        const clickEnIcono = e.target.closest(".icon-three");
        const clickEnSubmenu = e.target.closest(".submenu-options");
        if (!clickEnIcono && !clickEnSubmenu) {
            cerrarSubmenu();
        }
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && actualIconSubmenu) {
            actualIconSubmenu.focus();
            cerrarSubmenu();
        }
    });

    function posicionar(iconSubmenu, submenuOptions) {
        // Posicionamos el submenu con "fixed" respecto al ícono (no "absolute"
        // respecto al contenedor) para que no lo recorte el scroll horizontal
        // de la tabla (.table-wrapper tiene overflow-x: auto). Se mide DESPUÉS
        // de mostrarlo (con .active ya puesto) para conocer su alto real y
        // decidir si abre hacia abajo o hacia arriba del ícono.
        const rect = iconSubmenu.getBoundingClientRect();
        const menuWidth = submenuOptions.offsetWidth || 200;
        const menuHeight = submenuOptions.offsetHeight;

        let top = rect.bottom + 4;
        if (top + menuHeight > window.innerHeight) {
            // No entra hacia abajo: lo abrimos hacia arriba del ícono.
            top = Math.max(8, rect.top - menuHeight - 4);
        }

        let left = rect.right - menuWidth;
        left = Math.max(8, Math.min(left, window.innerWidth - menuWidth - 8));

        submenuOptions.style.position = "fixed";
        submenuOptions.style.top = `${top}px`;
        submenuOptions.style.left = `${left}px`;
    }

    // Recorremos todos los iconos y les agregarmos submenus en el clic
    iconSubmenuList.forEach((iconSubmenu) => {
        iconSubmenu.addEventListener("click", (e) => {
            const submenuOptions = iconSubmenu.nextElementSibling;

            // Si el iconSubmenu actual ya tiene la clase "active", lo cerramos
            if (iconSubmenu === actualIconSubmenu) {
                cerrarSubmenu();
            } else {
                // Si no, cerramos el submenú anterior (si lo hay)
                cerrarSubmenu();
                // Lo mostramos primero para poder medir su alto real...
                submenuOptions.classList.add("active");
                // ...y recién ahí lo posicionamos.
                posicionar(iconSubmenu, submenuOptions);
                actualIconSubmenu = iconSubmenu;
                iconSubmenu.setAttribute("aria-expanded", "true");
            }

            e.stopPropagation();
        });
    });
});
