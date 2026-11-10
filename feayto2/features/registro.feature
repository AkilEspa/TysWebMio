# language: es

Característica: Registro de cuenta

  Escenario: Creacion correcta
    Dado que el usuario está en la página de registro
    Cuando escribe "Akil" en "nombre"
    Y escribe "Espadas Jiménez" en "apellidos"
    Y escribe "12345Aa@" en "password"
    Y escribe "12345Aa@" en "confirmPassword"
    Y escribe "Ciudad Real" en "municipio"
    Y introduce un correo electrónico único de prueba
    Y pulsa el botón de registrar
    Entonces se muestra un mensaje de éxito
