package com.travelgo.model;

/*
    Representa un usuario del sistema
*/

public class Usuario {
    private final String id;
    private String nombre;
    private String correo;
    private String password;
    private RolUsuario rol;
    private String telefono;
    private boolean notificacionesCorreo;
    private boolean notificacionesSms;


    /**
     * Constructor compatible con las implementaciones anteriores.
     */
    public Usuario(String id, String nombre, String correo,
                   String password, RolUsuario rol) {
        this(id, nombre, correo, password, rol, null, false, false);
    }
    /*
        Construye un usuario con sus datos principales
    */
    public Usuario(String id, String nombre, String correo,
                   String password, RolUsuario rol, String telefono,
                   boolean notificacionesCorreo,
                   boolean notificacionesSms) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "El id del usuario no puede ser nulo o vacío");
        }

        this.id = id;
        setNombre(nombre);
        setCorreo(correo);
        setPassword(password);
        setRol(rol);
        setTelefono(telefono);
        setNotificacionesCorreo(notificacionesCorreo);
        setNotificacionesSms(notificacionesSms);
    }

    /*Obtiene el ID del usuario */
    public String getId() {
        return id;
    }

    /*Obtiene el nombre del usuario */
    public String getNombre() {
        return nombre;
    }

    /*Establece el nombre del usuario */
    public void setNombre(String nombre) {
        if( nombre == null || nombre.isBlank()){
            throw new IllegalArgumentException(
                "El nombre del usuario no puede ser nulo o vacío");
        }
        this.nombre = nombre;
    }

    /*Obtiene el correo del usuario */
    public String getCorreo() {
        return correo;
    }

    /*Establece el correo del usuario */
    public void setCorreo(String correo) {
        if( correo == null || correo.isBlank()){
            throw new IllegalArgumentException(
                "El correo del usuario no puede ser nulo o vacío");
        }
        this.correo = correo;
    }

    /*Obtiene la contraseña del usuario */
    public String getPassword() {
        return password;
    }

    /*Establece la contraseña del usuario */
    public void setPassword(String password) {
        if( password == null || password.isBlank()){
            throw new IllegalArgumentException(
                "La contraseña del usuario no puede ser nula o vacía");
        }
        this.password = password;
    }

    /*Obtiene el rol del usuario */
    public RolUsuario getRol() {
        return rol;
    }

    /*Establece el rol del usuario */
    public void setRol(RolUsuario rol) {
        if( rol == null ){
            throw new IllegalArgumentException(
                "El rol del usuario debe ser obligatorio");
        }
        this.rol = rol;
    }

    /*Obtiene el teléfono del usuario */
    public String getTelefono() {
        return telefono;
    }

    /*Establece el teléfono del usuario */
    public void setTelefono(String telefono) {
        this.telefono = (telefono ==  null || telefono.isBlank()) ? null : telefono.trim();

        if(this.telefono == null && this.notificacionesSms){
            notificacionesSms = false;
        }
    }

    /*Obtiene si el usuario desea recibir notificaciones por correo */
    public boolean isNotificacionesCorreo() {
        return notificacionesCorreo;
    }

    /*Establece si el usuario desea recibir notificaciones por correo */
    public void setNotificacionesCorreo(boolean notificacionesCorreo) {
        this.notificacionesCorreo = notificacionesCorreo;
    }

    /*Obtiene si el usuario desea recibir notificaciones por SMS */
    public boolean isNotificacionesSms() {
        return notificacionesSms;
    }

    /*Establece si el usuario desea recibir notificaciones por SMS */
    public void setNotificacionesSms(boolean notificacionesSms) {
        if(notificacionesSms && telefono == null){
            throw new IllegalArgumentException(
                "Se necesita un telefono para activar las notificaciones SMS");
        }

        this.notificacionesSms = notificacionesSms;
    }
}