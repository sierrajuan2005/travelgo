# Anuncio de TravelGo (ExpoSoftware)

Video publicitario de 30 s en dos formatos: **horizontal 16:9** (pantallas y presentaciones) y
**vertical 9:16** (Reels, TikTok, historias). Se genera en el navegador, sin programas de edición:
la página dibuja cada cuadro en un `<canvas>`, lo codifica con WebCodecs (video H.264 y audio AAC) y
arma el MP4 con [mp4-muxer](https://github.com/Vanilagy/mp4-muxer).

Los MP4 no se guardan en Git (pesan ~50 MB cada uno): se publican como adjunto del *Release*.

## Contenido

| Tiempo | Escena |
|---|---|
| 0–4 s | "¿LISTO · PARA · TU · PRÓXIMO · VIAJE?" palabra por palabra, al ritmo |
| 4–10 s | Cartagena, Cusco y Cancún con fotos reales y el nombre gigante |
| 10–14 s | "Conoce a Niko", la mascota |
| 14–22 s | Capturas reales de la app en celulares (inicio, detalle con clima, pago con cupón, factura) |
| 22–24 s | 4 destinos · 5 APIs · 1 clic · 0 estrés |
| 24–30 s | "Tu próximo destino está a un clic" y cierre "Descúbrenos en ExpoSoftware" |

## Cómo volver a generarlo

1. Opcional: poner una canción libre de derechos como `musica.mp3` en esta carpeta. La página detecta su
   BPM, busca la parte más fuerte y sincroniza los cortes. Sin canción usa un beat propio.
2. Opcional: `niko_sin_fondo.png` (Niko sin fondo, por ejemplo con Paint → Imagen → Quitar fondo sobre
   `niko_zona.png`). Sin ese archivo, Niko aparece como sticker.
3. Desde esta carpeta: `python servidor.py .` y abrir <http://127.0.0.1:8765/anuncio.html> en Chrome.
4. "Vista previa con sonido" para revisarlo; "Exportar los dos MP4" guarda
   `TravelGo-anuncio-horizontal.mp4` y `TravelGo-anuncio-vertical.mp4` en esta carpeta.

Las capturas de `capturas/` se tomaron de la app corriendo en local, en tamaño de celular (430 px).

## Créditos

- Mascota Niko: imagen del equipo (`niko.png`).
- Fotos de Wikimedia Commons: Cartagena — poirpom (CC BY 2.0); Cusco — Allison Bellido (CC BY-SA 4.0);
  Cancún — dronepicr (CC BY 2.0). El crédito aparece en el video.
- Letras: Bricolage Grotesque, Geist e Instrument Serif (Google Fonts, licencia OFL).
- Efectos de sonido y beat de respaldo: generados en la página con Web Audio.
