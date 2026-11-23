package com.travelgo.expo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

/**
 * Dibuja un código QR como SVG con ZXing. Se genera en el servidor (no en el navegador) para que la pantalla
 * del stand funcione aunque no haya internet: no depende de ninguna librería en un CDN.
 */
public final class GeneradorQr {

    private GeneradorQr() {
    }

    public static String svg(String texto) {
        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo generar el código QR de " + texto, e);
        }

        int ancho = matriz.getWidth();
        int alto = matriz.getHeight();
        // Un solo <path> con un cuadrito de 1x1 por módulo negro: liviano y nítido a cualquier tamaño
        StringBuilder trazo = new StringBuilder();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (matriz.get(x, y)) {
                    trazo.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + ancho + " " + alto
                + "\" shape-rendering=\"crispEdges\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                + "<path fill=\"#0b0820\" d=\"" + trazo + "\"/></svg>";
    }
}
