# language: es
@feature
Característica: Reserva de varias habitaciones para un mismo cliente
  Como recepcionista
  quiero reservar varias habitaciones a la vez para un grupo
  para que la reserva se guarde completa o no se guarde
  (HU47 y HU49; reglas RN01, RN02, RN03, RN24 y RN30)

  Antecedentes:
    Dado una categoría "Familiar QA" con precio 260,0
    Y una habitación "Q11" de la categoría "Familiar QA" con aforo 3
    Y una habitación "Q12" de la categoría "Familiar QA" con aforo 3
    Y un cliente "Karen Díaz" con documento "88000002"
    Y el administrador "qa_admin" con sesión iniciada

  Escenario: Una reserva válida crea una estadía por habitación y las ocupa
    Cuando reservo para el cliente "88000002" las habitaciones "Q11,Q12" el "2027-06-10T14:00" por 3 noches con "3,2" huéspedes
    Entonces se crean 2 estadías activas para el cliente "88000002"
    Y la habitación "Q11" queda "OCUPADA"
    Y la habitación "Q12" queda "OCUPADA"

  Escenario: Si una habitación supera el aforo no se guarda ninguna
    Cuando reservo para el cliente "88000002" las habitaciones "Q11,Q12" el "2027-06-10T14:00" por 3 noches con "2,5" huéspedes
    Entonces se muestra el error "tiene aforo para 3 persona(s), y pediste 5."
    Y no se crea ninguna estadía nueva
    Y la habitación "Q11" queda "DISPONIBLE"

  Escenario: Si una habitación ya está tomada en esas fechas no se guarda ninguna
    Dado una estadía activa del cliente "88000002" en la habitación "Q12" el "2027-06-11T12:00" por 2 noches con tarifa 520,0 y pago 0,0
    Cuando reservo para el cliente "88000002" las habitaciones "Q11,Q12" el "2027-06-10T14:00" por 3 noches con "2,2" huéspedes
    Entonces se muestra el error "La habitación Q12 ya tiene otra reserva activa que se cruza con esas fechas."
    Y no se crea ninguna estadía nueva
