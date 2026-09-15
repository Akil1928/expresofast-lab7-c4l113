package cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto;

import java.math.BigDecimal;

public class EnvioResponseDTO {

    private Integer id;
    private String codigoRastreo;
    private String direccionDestino;
    private BigDecimal pesoKg;
    private BigDecimal costo;
    private String estadoEnvio;
    private String placaVehiculo;
    private String nombreConductor;

    public EnvioResponseDTO(Integer id, String codigoRastreo, String direccionDestino,
                             BigDecimal pesoKg, BigDecimal costo, String estadoEnvio,
                             String placaVehiculo, String nombreConductor) {
        this.id = id;
        this.codigoRastreo = codigoRastreo;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
        this.costo = costo;
        this.estadoEnvio = estadoEnvio;
        this.placaVehiculo = placaVehiculo;
        this.nombreConductor = nombreConductor;
    }

    public Integer getId() { return id; }
    public String getCodigoRastreo() { return codigoRastreo; }
    public String getDireccionDestino() { return direccionDestino; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public BigDecimal getCosto() { return costo; }
    public String getEstadoEnvio() { return estadoEnvio; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public String getNombreConductor() { return nombreConductor; }
}