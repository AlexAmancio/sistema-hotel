# language: es
@feature
Característica: Inicio de sesión y permisos
  Como administrador de Tunki Lodge
  quiero que solo el personal autorizado entre al sistema
  para proteger los datos de los huéspedes
  (HU01, HU10 y HU13; reglas RN11, RN21 y RN35)

  Escenario: Un usuario activo entra con su contraseña
    Dado un usuario de recepción "qa_recepcion" con contraseña "Recepcion2026"
    Cuando inicio sesión como "qa_recepcion" con contraseña "Recepcion2026" desde la IP "10.90,0.1"
    Entonces entra al sistema

  Escenario: Con la contraseña incorrecta vuelve al inicio de sesión
    Dado un usuario de recepción "qa_recepcion" con contraseña "Recepcion2026"
    Cuando inicio sesión como "qa_recepcion" con contraseña "otra" desde la IP "10.90,0.2"
    Entonces vuelve al inicio de sesión con el aviso "Verifique sus credenciales"

  Escenario: Un usuario desactivado no puede entrar
    Dado un usuario desactivado "qa_inactivo" con contraseña "Inactivo2026"
    Cuando inicio sesión como "qa_inactivo" con contraseña "Inactivo2026" desde la IP "10.90,0.3"
    Entonces vuelve al inicio de sesión con el aviso "Verifique sus credenciales"

  Escenario: Tres intentos fallidos desde la misma IP la bloquean aunque luego la contraseña sea correcta
    Dado un usuario de recepción "qa_recepcion" con contraseña "Recepcion2026"
    Cuando fallo 3 veces la contraseña de "qa_recepcion" desde la IP "10.90,0.4"
    Y inicio sesión como "qa_recepcion" con contraseña "Recepcion2026" desde la IP "10.90,0.4"
    Entonces vuelve al inicio de sesión con el aviso "Vuelva a intentar en"

  Escenario: Recepción no puede crear usuarios
    Dado la recepcionista "qa_recepcion" con sesión iniciada
    Cuando intento abrir "/usuarios/nuevo/"
    Entonces el acceso es denegado

  Escenario: Recepción no puede crear productos del catálogo
    Dado la recepcionista "qa_recepcion" con sesión iniciada
    Cuando intento abrir "/productos-consumo/nuevo/"
    Entonces el acceso es denegado
