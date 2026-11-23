# language: es
@aceptacion
Característica: Registro del check-in
  Como recepcionista de Tunki Lodge
  quiero registrar la llegada de un huésped
  para que la habitación quede ocupada y la tarifa se calcule sola
  (HU30, HU34 y HU37; reglas RN01, RN02, RN04, RN24 y RN42)

  Antecedentes:
    Dado una categoría "Matrimonial QA" con precio 180,0
    Y una habitación "Q01" de la categoría "Matrimonial QA" con aforo 2
    Y un cliente "Carmen Herrera" con documento "88000001"
    Y el administrador "qa_admin" con sesión iniciada

  Escenario: Un check-in válido ocupa la habitación y calcula la tarifa
    Cuando registro un check-in del cliente "88000001" en la habitación "Q01" el "2027-05-03T12:00" por 2 noches con 2 huéspedes
    Entonces la estadía queda "ACTIVA" con tarifa 360,0
    Y la habitación "Q01" queda "OCUPADA"
    Y la estadía queda a nombre de "qa_admin"

  Escenario: No se aceptan más huéspedes que el aforo de la habitación
    Cuando registro un check-in del cliente "88000001" en la habitación "Q01" el "2027-05-03T12:00" por 2 noches con 3 huéspedes
    Entonces se muestra el error "supera el aforo máximo de la habitación Q01 (2 personas)"
    Y no se crea ninguna estadía nueva

  Escenario: No se acepta un check-in que se cruza con otra estadía activa
    Dado una estadía activa del cliente "88000001" en la habitación "Q01" el "2027-05-03T12:00" por 3 noches con tarifa 540,0 y pago 0,0
    Cuando registro un check-in del cliente "88000001" en la habitación "Q01" el "2027-05-04T12:00" por 1 noches con 1 huéspedes
    Entonces se muestra el error "La habitación Q01 ya tiene otra reserva activa que se cruza con esas fechas."
    Y no se crea ninguna estadía nueva

  Escenario: En temporada alta se cobra el precio de temporada
    Dado una categoría "Domo QA" con precio 300,0 y temporada alta a 380,0 del "2027-12-20" al "2028-01-05"
    Y una habitación "Q02" de la categoría "Domo QA" con aforo 4
    Cuando registro un check-in del cliente "88000001" en la habitación "Q02" el "2027-12-26T14:00" por 3 noches con 4 huéspedes
    Entonces la estadía queda "ACTIVA" con tarifa 1140,0
