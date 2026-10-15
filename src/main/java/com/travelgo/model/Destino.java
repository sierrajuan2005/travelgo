package com.travelgo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Destino turístico del catálogo de la agencia.
 */
@Entity
@Table(name = "destino")
public class Destino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @NotBlank(message = "El país es obligatorio")
    @Column(nullable = false, length = 80)
    private String pais;

    @NotBlank(message = "La ciudad es obligatoria")
    @Column(nullable = false, length = 80)
    private String ciudad;

    public Destino() {
    }

    public Destino(String nombre, String descripcion, String pais, String ciudad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.pais = pais;
        this.ciudad = ciudad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
}