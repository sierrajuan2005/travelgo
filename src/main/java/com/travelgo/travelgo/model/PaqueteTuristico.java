package com.travelgo.travelgo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Paquete turístico ofrecido por la agencia. Puede incluir uno o varios destinos.
 */
@Entity
@Table(name = "paquete_turistico")
public class PaqueteTuristico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Min(value = 1, message = "La duración mínima es de 1 día")
    @Column(nullable = false)
    private int duracionDias;

    @Column(nullable = false)
    private boolean activo = true;

    // Relación con Destino: confirmar con el ER de Integrante 1
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "paquete_destino",
            joinColumns = @JoinColumn(name = "paquete_id"),
            inverseJoinColumns = @JoinColumn(name = "destino_id"))
    private List<Destino> destinos = new ArrayList<>();

    public PaqueteTuristico() {
    }

    public PaqueteTuristico(String nombre, String descripcion, BigDecimal precio, int duracionDias) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.duracionDias = duracionDias;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public int getDuracionDias() { return duracionDias; }
    public void setDuracionDias(int duracionDias) { this.duracionDias = duracionDias; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public List<Destino> getDestinos() { return destinos; }
    public void setDestinos(List<Destino> destinos) { this.destinos = destinos; }
}