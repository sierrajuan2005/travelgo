package com.travelgo.model;

/*
    Representa la abstraccion de un servicio turistico que puede incluir una reserva
*/
public abstract  class ServicioReservable {
    private final String id;
    private final double precio;
    private final boolean disponible;
    
    /*
        Construye un servicio reservable
        param id identificador unico del servicio
        param precio precio base del servicio
        param disponible indica si el servicio esta disponible
        throws IllegalArgumentException si el identificador es nulo o vacio, o si el precio es negativo o nulo.
    */

    protected ServicioReservable(String id, double precio, boolean disponible){
        if(id==null || id.isBlank()){
            throw new IllegalArgumentException(
                "El identificador del servicio es obligatorio."
            );
        }
        if(!Double.isFinite(precio) || precio<0){
            throw new IllegalArgumentException(
                "El precio debe ser un numero finito mayor o igual que cero."
            );
        }
        this.id=id.trim();
        this.precio=precio;
        this.disponible=disponible;
        }
        
        /*
            Calcula el costo total del servicio
            return costo total del servicio
        */
        public abstract double calcularCosto();


        /*
            Obtiene el identificador del servicio
            return identificador del servicio
        */
        public String getId(){
            return id;
        }

        /*
            Obtiene el precio del servicio
            return precio del servicio
        */
        public double getPrecio(){
            return precio;
        }

        /*
            Consulta el estado de disponibilidad del servicio
            return true si el servicio está disponible, false en caso contrario
        */
        public boolean isDisponible(){
            return disponible;
        }

    
}
