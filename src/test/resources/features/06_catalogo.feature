# language: es
@feature
Característica: Catálogo de productos de consumo
  Como administrador
  quiero mantener el catálogo de productos
  para que recepción cargue consumos con un clic
  (HU52, HU57 y HU58; reglas RN10 y RN28)

  Antecedentes:
    Dado una categoría "Matrimonial QA" con precio 180,0
    Y una habitación "Q31" de la categoría "Matrimonial QA" con aforo 2
    Y un cliente "Carmen Herrera" con documento "88000020"
    Y el administrador "qa_admin" con sesión iniciada
    Y una estadía activa del cliente "88000020" en la habitación "Q31" el "2027-08-01T10:00" por 2 noches con tarifa 360,0 y pago 0,0

  Escenario: Solo los productos activos aparecen como atajos
    Dado un producto del catálogo "Snack QA activo" a 9,5 "activo"
    Y un producto del catálogo "Snack QA inactivo" a 9,5 "inactivo"
    Cuando abro los consumos de la estadía
    Entonces el producto "Snack QA activo" aparece entre los atajos
    Y el producto "Snack QA inactivo" no aparece entre los atajos

  Escenario: Un producto ya usado en consumos no se puede eliminar
    Dado un producto del catálogo "Snack QA" a 9,5 "activo"
    Y cargo el producto "Snack QA" desde su atajo a la estadía
    Cuando intento eliminar el producto "Snack QA"
    Entonces se muestra el error "No se puede eliminar"
    Y el producto "Snack QA" sigue en el catálogo
