# language: es

Característica: Inicio de sesión

  Como usuario registrado
  Quiero iniciar sesión
  Para acceder a las funcionalidades de la aplicación

  Escenario: Inicio de sesión correcto
    Dado que el usuario está en la página de inicio de sesión
    Cuando escribe "akil.espadas@gmail.com" en "email"
    Y escribe "12345Aa@" en "password"
    Y pulsa el botón de iniciar sesión
    Entonces las credenciales son aceptadas para iniciar sesión

Escenario: Inicio de sesión con credenciales incorrectas
  Dado que el usuario está en la página de inicio de sesión
  Cuando introduce credenciales incorrectas
  Y pulsa el botón de iniciar sesión
  Entonces permanece en la página de inicio de sesión
  Y se muestra un mensaje de error
  Y no se crea un token de sesión
