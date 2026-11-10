# LimaTur - Grupo 2

Aplicación Android nativa desarrollada en Kotlin para explorar puntos turísticos de Lima.

## Funcionalidades
- Mapa Google Maps con agrupación de marcadores (Marker Clustering).
- Catálogo local de 50 lugares turísticos de Lima.
- Filtros combinados por categoría y distrito.
- Búsqueda con espera de 300 ms (debounce).
- Estilo de mapa diurno/nocturno.
- Zoom a los elementos de un grupo usando LatLngBounds.

## Configuración de Google Maps
1. Crear una API key en Google Cloud Console y habilitar Maps SDK for Android.
2. En GitHub, ir a Settings > Secrets and variables > Actions.
3. Crear un repository secret llamado MAPS_API_KEY con la clave.
4. Ejecutar Actions > Build LimaTur APK > Run workflow.

Sin una API key válida el APK puede compilar, pero Google Maps mostrará un mapa vacío o un error de autorización.

## Descargar
Al terminar el workflow, abrir la ejecución en Actions y descargar el artefacto LimaTur-Grupo2-APK.
