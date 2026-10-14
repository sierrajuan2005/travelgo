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
}
