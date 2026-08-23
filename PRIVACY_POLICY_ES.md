# Política de privacidad

Esta política se aplica a The Lexorcist (la «Aplicación»), creada por Hereliesaz
(el «Proveedor del Servicio») y ofrecida de forma gratuita, «TAL CUAL».

La Aplicación maneja pruebas legales, que pueden incluir material amparado por
el secreto profesional entre abogado y cliente. Esta política describe
exactamente qué recopila la Aplicación, dónde se almacena y qué sale de su
dispositivo, para que usted pueda decidir qué introducir en ella.

**En vigor desde el 23-08-2026.** Sustituye a una versión anterior que afirmaba
que la Aplicación «no recopila ninguna información de identificación personal» y
que mencionaba Google Drive como único servicio de terceros. Ambas afirmaciones
eran inexactas; las correcciones se describen a continuación.

## Qué recopila la Aplicación

Todo lo siguiente se recopila únicamente cuando usted decide capturarlo o
importarlo.

**Pruebas que usted captura o importa**

- Fotografías y capturas de pantalla, y el texto extraído de ellas.
- Grabaciones de audio y vídeo, y las transcripciones que se generen.
- Documentos y notas de texto libre que usted introduzca.

**Datos que importa desde su dispositivo**, cada uno tras un permiso
independiente que puede rechazar:

- **Mensajes SMS**: remitente, destinatario, contenido y fecha, para los
  contactos e intervalos de fechas que usted seleccione.
- **Registros de llamadas**: número, sentido, fecha y duración.
- **Ubicación precisa**: una posición asociada a un elemento de prueba, y el
  historial de ubicaciones que importe desde un archivo de Google Takeout.
- **Fotos, vídeo y audio** del almacenamiento de su dispositivo.
- **Cámara**.

**Correo que importa**, al conectar una cuenta: encabezados, cuerpos y adjuntos
de Gmail, Outlook o cualquier buzón IMAP que configure.

**Información del caso que usted introduce**: partes, tribunal, número de caso,
jurisdicción, alegaciones, pruebas documentales, etiquetas y comentarios.

Los SMS, los registros de llamadas, la ubicación precisa y el correo son
información de identificación personal, y afectan tanto a otras personas como a
usted. Importarlos es su decisión y su responsabilidad.

## Dónde se almacena

Todos los datos del caso se almacenan **en su dispositivo**, en un archivo de
base de datos llamado `lexorcist_data.xlsx` situado en el almacenamiento privado
de la Aplicación, junto con los archivos de prueba originales.

La base de datos está **cifrada en reposo** mediante una clave alojada en el
almacén de claves por hardware de su dispositivo. Los archivos de prueba
originales (fotografías, audio, vídeo) se guardan en el directorio privado de la
Aplicación pero **todavía no se cifran individualmente**; están protegidos por
el aislamiento de aplicaciones de Android, que impide que otras aplicaciones los
lean, y por el cifrado del dispositivo.

Los datos del caso están **excluidos de la copia de seguridad automática de
Android** y de la transferencia entre dispositivos. No se copian a la copia de
seguridad de su cuenta de Google.

## Qué sale de su dispositivo

Nada sale de su dispositivo salvo que usted lo active.

**Sincronización en la nube (opcional).** Si la activa, la base de datos y las
carpetas de sus casos se suben al proveedor que elija y en el que inicie sesión:
Google Drive o Dropbox. El Proveedor del Servicio no tiene acceso a ese
almacenamiento.

**Acceso a la cuenta de Google.** Iniciar sesión con Google concede a la
Aplicación:

- Acceso completo a su Google Drive (lectura, escritura y borrado de **todos**
  los archivos de esa cuenta, no solo de los creados por la Aplicación).
- Acceso completo a sus Hojas de cálculo de Google.
- Acceso de solo lectura a su Gmail.

Si inicia sesión con una cuenta que contiene material de otros clientes, tenga
en cuenta que la Aplicación posee una credencial lo bastante amplia para
alcanzarlo. Restringir esto a los archivos creados por la Aplicación es una
tarea pendiente conocida.

**IA en la nube (opcional, mediante scripts).** El motor de scripts expone una
función, `lex.ai.generate`, que envía texto a la API Gemini de Google. Un script
que usted escriba o instale puede pasarle el texto de una prueba. Si ejecuta un
script así, ese contenido se transmite a Google. Ningún script lo hace salvo que
esté escrito para ello.

**Google Apps Script (opcional, mediante scripts).** Un script puede invocar
`lex.google.runAppsScript`, que llama a la API de Google Apps Script con su
credencial.

**Scripts y plantillas compartidos.** La Aplicación puede descargar scripts y
plantillas que otras personas han publicado en una hoja de cálculo pública
compartida, y publicar los suyos. Los elementos publicados llevan el nombre y el
correo electrónico que usted indique. **Los scripts compartidos no se revisan,
no se firman ni se aíslan del acceso a la red**: un script obtenido así se
ejecuta con las dos capacidades anteriores. Trate su instalación como la
ejecución de cualquier código no confiable sobre sus archivos del caso.

El procesamiento local —OCR, voz a texto y similitud de textos— se ejecuta en el
dispositivo y no envía nada a ninguna parte.

## Servicios de terceros

Según las funciones que utilice:

- [Google (Drive, Sheets, Gmail, Apps Script, Firebase, Gemini)](https://www.google.com/policies/privacy/)
- [Dropbox](https://www.dropbox.com/privacy)
- [Microsoft (OneDrive, Outlook)](https://privacy.microsoft.com/privacystatement)

El Proveedor del Servicio no opera ningún servidor y no recibe ningún dato de la
Aplicación.

## Seguridad

La base de datos se cifra en reposo con una clave respaldada por hardware. Las
credenciales OAuth se cifran por separado. La copia de seguridad automática está
desactivada.

Ningún método de almacenamiento o transmisión es completamente seguro, y el
Proveedor del Servicio no puede garantizar una seguridad absoluta. En particular:

- Los archivos de prueba originales no se cifran individualmente.
- Un dispositivo rooteado, comprometido o desbloqueado en manos de otra persona
  no ofrece ninguna protección.
- Todo lo que sincronice con un proveedor en la nube queda sujeto a la seguridad
  de ese proveedor y a las credenciales de esa cuenta.

## Su control

- Cada importación está sujeta a un permiso que puede rechazar.
- La sincronización en la nube está desactivada salvo que usted la active.
- Eliminar un caso borra sus datos del dispositivo. Las copias ya sincronizadas
  con un proveedor en la nube deben borrarse allí.
- Desinstalar elimina todos los datos locales de los casos. No hay copia de
  seguridad, por diseño.

## Cambios en esta política

El Proveedor del Servicio puede actualizar esta política. Los cambios
sustanciales se publicarán aquí, actualizando la fecha de entrada en vigor.

## Contacto

Preguntas o correcciones: abra una incidencia en el repositorio del proyecto.
