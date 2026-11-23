# language: es
@aceptacion
Característica: Consumos, check-out y comprobante
  Como recepcionista
  quiero cargar los consumos, cerrar la estadía y emitir el comprobante
  para cobrar el monto exacto y entregar el detalle con IGV
  (HU36, HU41 a HU44, HU51, HU53 y HU59; reglas RN05 a RN09 y RN17)

  Antecedentes:
    Dado una categoría "Matrimonial QA" con precio 180,0
    Y una habitación "Q21" de la categoría "Matrimonial QA" con aforo 2
    Y un cliente "Carmen Herrera" con documento "88000003"
    Y el administrador "qa_admin" con sesión iniciada
    Y una estadía activa del cliente "88000003" en la habitación "Q21" el "2027-07-01T10:00" por 2 noches con tarifa 360,0 y pago 180,0

  Escenario: Un consumo se suma al total y al saldo pendiente
    Cuando agrego el consumo "Minibar" de 20,0 a la estadía
    Entonces el total de la estadía es 380,0 y el saldo pendiente es 200,0

  Escenario: Un consumo sin monto válido no se registra
    Cuando agrego el consumo "Minibar" de 0,0 a la estadía
    Entonces se muestra el error "El monto debe ser mayor a cero."
    Y la estadía tiene 0 consumos

  Escenario: Una estadía con consumos no se puede cancelar
    Dado agrego el consumo "Minibar" de 20,0 a la estadía
    Cuando intento cancelar la estadía
    Entonces se muestra el error "No se puede cancelar: ya tiene consumos cargados"
    Y la estadía sigue "ACTIVA"

  Escenario: Cancelar una estadía sin consumos manda la habitación a limpieza
    Cuando intento cancelar la estadía
    Entonces la estadía sigue "CANCELADA"
    Y la habitación "Q21" queda "LIMPIEZA"

  Escenario: Un no-show libera la habitación sin pasar por limpieza
    Cuando marco la estadía como no-show
    Entonces la estadía sigue "NO_SHOW"
    Y la habitación "Q21" queda "DISPONIBLE"

  Escenario: El check-out cierra la estadía y manda la habitación a limpieza
    Cuando hago el check-out de la estadía
    Entonces la estadía sigue "FINALIZADA"
    Y se registra la hora real de salida
    Y la habitación "Q21" queda "LIMPIEZA"

  Escenario: Después del check-out no se pueden cargar consumos
    Dado hago el check-out de la estadía
    Cuando agrego el consumo "Lavandería" de 28,0 a la estadía
    Entonces se muestra el error "Esta estadía ya no está activa"
    Y la estadía tiene 0 consumos

  Escenario: El comprobante exige check-out
    Cuando emito el comprobante de la estadía
    Entonces se muestra el error "No se puede emitir comprobante: la estadía todavía no tiene check-out hecho."

  Escenario: El comprobante desglosa el IGV incluido en los precios y no se duplica
    Dado agrego el consumo "Minibar" de 20,0 a la estadía
    Y hago el check-out de la estadía
    Cuando emito el comprobante de la estadía
    Y emito el comprobante de la estadía
    Entonces el comprobante tiene serie "C001", valor de venta 322,03, IGV 57,97 y total 380,0
    Y la estadía tiene un solo comprobante
