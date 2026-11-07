# Inkr8 — publicación de pruebas y capítulo 10

Publicación del 6 de noviembre de 2026 en la rama `pruebas/capitulo-10`, mediante la sesión de GitHub de `MACOABC`. Se conserva el historial existente. Los cambios ejecutables publicados corresponden a pruebas, fixtures, runners y configuración de cobertura; las correcciones de producto se adjuntan como propuestas separadas y no se aplican a las fuentes de producción.

El grupo es responsable de la revisión y entrega, elaboradas con asistencia de IA declarada. La autoría técnica de los commits no acredita aportes, revisión o reproducción individual de cada integrante. La confirmación docente y las decisiones de negocio pendientes conservan su estado.

## Archivos para revisión

- [Capítulo 10 editable con evidencias](Inkr8_Capitulo_10_Con_Evidencias.docx): listo para copiar al informe principal. Incluye resultados, defectos y pendientes, con tres capturas de ejecuciones aprobadas.
- [Capturas de resultados aprobados](Inkr8_Evidencias_Resultados_Aprobados.docx): ocho capturas auténticas de reportes o registros originales. Cada figura distingue base, propuesta y dependencias controladas.
- [Expediente completo de cierre](Entrega_Inkr8_Capitulo_10_Cierre_Final.zip) y [SHA-256](Entrega_Inkr8_Capitulo_10_Cierre_Final.sha256.txt): código, parches, seis fichas, CFG, matrices, cobertura, registros, capturas y comandos de reproducción. Extraer el ZIP y leer `README_Entrega.md` y `README_Ejecucion.md`.
- [Propuesta de desempate de matchmaking](Inkr8_Propuesta_Backend_Contencion_Empates.patch): validada en copia aislada, pendiente de decisión de incorporación.
- [Propuesta de mensajes del cliente](Inkr8_Propuesta_Cliente_Mensajes.patch): validada en copia aislada, pendiente de decisión de incorporación.

Los dos Word se elaboraron después del ZIP y se entregan junto a él. Las rutas de las evidencias citadas en los documentos se resuelven dentro del ZIP extraído. Las frases históricas «No se publicó» del expediente describen el cierre técnico previo a esta publicación; este archivo documenta la publicación posterior.

## Resultados que conserva la entrega

La base registra 323 casos únicos: 313 aprobados y 10 fallidos. El backend obtiene 116/116 de unidad/adaptador y 100/100 funcionales. La base conserva los fallos de carga, mensajes y conformidad 429/503 documentados en 10.5; el conteo de tests no es un porcentaje de trabajo terminado ni aceptación integral de HU.

En la propuesta aislada de matchmaking pasan tres perfiles de 50 solicitudes con 25 parejas, pico 50 y cero inconsistencias; el perfil impar de 51 completa 51 llamadas y deja una partida pendiente legítima. La propuesta aislada del cliente obtiene 96/96 JVM, 6/6 UI y 2/2 SDK local. Estos resultados no se mezclan con los de la base. La ventana de 100 candidatos es una limitación heredada.

El alcance sigue limitado a las 19 HU prioritarias y HU 2.2 justificada. Permanecen las decisiones sobre ELO, recuperación 429/503, mensajes, fronteras de caja negra y aceptación docente, además de la participación individual y los accesos para Firebase/Google/OpenAI reales. El inventario único de responsables y evidencia necesaria está en `Pendientes_Externos.md` dentro del expediente.

No se desplegó el producto, no se editó el informe compartido y no se hicieron llamadas facturables a OpenAI.
