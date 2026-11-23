# language: es
@aceptacion
Característica: Registro de clientes
  Como recepcionista
  quiero registrar a los huéspedes una sola vez
  para no duplicar sus datos
  (HU15 y HU19; reglas RN14, RN29 y RN40)

  Antecedentes:
    Dado el administrador "qa_admin" con sesión iniciada

  Escenario: Los nombres se guardan con mayúscula inicial
    Cuando registro al cliente "cARMEN" "herrera rojas" con documento "88000010"
    Entonces el cliente con documento "88000010" queda guardado como "Carmen Herrera Rojas"

  Escenario: No se registra dos veces el mismo documento
    Dado un cliente "Carmen Herrera" con documento "88000011"
    Cuando registro al cliente "Otra" "Persona" con documento "88000011"
    Entonces se muestra el error "Ya existe un cliente con esa identificación."
    Y solo hay 1 cliente con el documento "88000011"

  Escenario: El alta rápida desde el check-in crea al cliente
    Cuando uso el alta rápida para "rápido" "qa" con documento "88000012"
    Entonces el cliente con documento "88000012" queda guardado como "Rápido Qa"
    Y la respuesta indica el nombre "Rápido Qa"

  Escenario: El alta rápida no duplica un documento existente
    Dado un cliente "Carmen Herrera" con documento "88000013"
    Cuando uso el alta rápida para "Otra" "Persona" con documento "88000013"
    Entonces se muestra el error "Ya existe un cliente con esa identificación."
    Y solo hay 1 cliente con el documento "88000013"
