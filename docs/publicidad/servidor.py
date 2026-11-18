"""Servidor local del anuncio de TravelGo.

Sirve anuncio.html y sus imágenes, y guarda el video que graba Chrome (POST /guardar?nombre=...)
en la carpeta de salida. Solo escucha en 127.0.0.1.
Uso: python -I servidor.py <carpeta_salida>
"""
import functools
import os
import re
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(AQUI, "salida")
os.makedirs(SALIDA, exist_ok=True)


class Manejador(SimpleHTTPRequestHandler):
    def do_POST(self):
        url = urlparse(self.path)
        if url.path != "/guardar":
            self.send_error(404)
            return
        nombre = parse_qs(url.query).get("nombre", ["anuncio.webm"])[0]
        if not re.fullmatch(r"[A-Za-z0-9_.-]{1,80}", nombre):
            self.send_error(400, "Nombre no valido")
            return
        largo = int(self.headers.get("Content-Length", "0"))
        datos = self.rfile.read(largo)
        ruta = os.path.join(SALIDA, nombre)
        with open(ruta, "wb") as archivo:
            archivo.write(datos)
        print(f"Guardado {ruta} ({len(datos)} bytes)", flush=True)
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.end_headers()
        self.wfile.write(ruta.encode("utf-8"))

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()


if __name__ == "__main__":
    servidor = ThreadingHTTPServer(("127.0.0.1", 8765), functools.partial(Manejador, directory=AQUI))
    print(f"Anuncio en http://127.0.0.1:8765/anuncio.html · videos en {SALIDA}", flush=True)
    servidor.serve_forever()
