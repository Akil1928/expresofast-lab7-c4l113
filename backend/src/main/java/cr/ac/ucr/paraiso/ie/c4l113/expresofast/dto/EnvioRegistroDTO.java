package cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de registro avanzado de un envio junto con sus paquetes asociados (Lab 11).
 * El campo "numeroTracking" del enunciado se mapea al "codigoRastreo" ya
 * existente en la entidad Envio (mismo concepto, mismo campo).
 */
public class EnvioRegistroDTO {

    @NotBlank(message = "El numero de rastreo es obligatorio.")
    @Pattern(regexp = "^EXP-\\d{4}$", message = "Formato invalido. Ejemplo: EXP-1234")
    private String numeroTracking;

    @NotBlank(message = "La direccion de destino es obligatoria.")
    private String direccionDestino;

    @NotNull(message = "El costo es obligatorio.")
    private BigDecimal costo;

    @NotNull(message = "La fecha de despacho es obligatoria.")
    private LocalDateTime fechaDespacho;

    @NotNull(message = "La fecha de entrega estimada es obligatoria.")
    private LocalDateTime fechaEntregaEstimada;

    @NotNull(message = "Debe indicar el vehiculo asignado.")
    private Integer vehiculoId;

    @NotNull(message = "Debe indicar el conductor asignado.")
    private Integer conductorId;

    @NotEmpty(message = "Debe registrar al menos un paquete.")
    @Valid
    private List<PaqueteDTO> paquetes;

    public EnvioRegistroDTO() {
    }

    public String getNumeroTracking() {
        return numeroTracking;
    }

    public void setNumeroTracking(String numeroTracking) {
        this.numeroTracking = numeroTracking;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(LocalDateTime fechaDespacho) {
        this.fechaDespacho = fechaDespacho;
    }

    public LocalDateTime getFechaEntregaEstimada() {
        return fechaEntregaEstimada;
    }

    public void setFechaEntregaEstimada(LocalDateTime fechaEntregaEstimada) {
        this.fechaEntregaEstimada = fechaEntregaEstimada;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Integer getConductorId() {
        return conductorId;
    }

    public void setConductorId(Integer conductorId) {
        this.conductorId = conductorId;
    }

    public List<PaqueteDTO> getPaquetes() {
        return paquetes;
    }

    public void setPaquetes(List<PaqueteDTO> paquetes) {
        this.paquetes = paquetes;
    }
}