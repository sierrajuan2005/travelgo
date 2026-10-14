package com.travelgo.travelgo.model;

/*
    Representa un usuario del sistema
*/

public class Usuario {
    private final String id;
    private String nombre;
    private String correo;
    private String password;
    private RolUsuario rol;

    /*
        Construye un usuario con sus datos principales
    */
    public Usuario(String id, String nombre, String correo, String password, RolUsuario rol) {
        if( id == null || id.isBlank()){
            throw new IllegalArgumentException(
                "El id del usuario no puede ser nulo o vacío");
        }
        this.id = id;
        setNombre(nombre);
        setCorreo(correo);
        setPassword(password);
        setRol(rol);
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
}