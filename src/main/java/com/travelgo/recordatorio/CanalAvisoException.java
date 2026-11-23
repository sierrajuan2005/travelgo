package com.travelgo.recordatorio;

/** El canal no pudo entregar el aviso (servidor de correo caído, proveedor de SMS rechazó el número...). */
public class CanalAvisoException extends RuntimeException {

    public CanalAvisoException(String mensaje) {
        super(mensaje);
    }

    public CanalAvisoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
