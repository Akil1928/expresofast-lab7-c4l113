package cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class EnvioRequestDTO {

    @NotBlank(message = "El código de rastreo es obligatorio")
    @Pattern(regexp = "^EXP-\\d{4}$", message = "Formato inválido. Ejemplo: EXP-1234")
    private String codigoRastreo;

    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a cero")
    private BigDecimal pesoKg;

    @NotNull(message = "El costo es obligatorio")
    @Positive(message = "El costo debe ser mayor a cero")
    private BigDecimal costo;

    @NotNull(message = "Debe indicar el vehículo")
    private Integer vehiculoId;

    @NotNull(message = "Debe indicar el conductor")
    private Integer conductorId;

    // ---- Getters y Setters ----
    public String getCodigoRastreo() { return codigoRastreo; }
    public void setCodigoRastreo(String codigoRastreo) { this.codigoRastreo = codigoRastreo; }
    public String getDireccionDestino() { return direccionDestino; }
    public void setDireccionDestino(String direccionDestino) { this.direccionDestino = direccionDestino; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
    public Integer getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Integer vehiculoId) { this.vehiculoId = vehiculoId; }
    public Integer getConductorId() { return conductorId; }
    public void setConductorId(Integer conductorId) { this.conductorId = conductorId; }
}