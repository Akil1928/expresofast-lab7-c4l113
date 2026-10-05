package cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para un paquete individual dentro del registro avanzado de un envio (Lab 11).
 */
public class PaqueteDTO {

    @NotBlank(message = "La descripcion del paquete es obligatoria.")
    private String descripcion;

    @NotNull(message = "El peso del paquete es obligatorio.")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a cero.")
    private BigDecimal pesoKg;

    public PaqueteDTO() {
    }

    public PaqueteDTO(String descripcion, BigDecimal pesoKg) {
        this.descripcion = descripcion;
        this.pesoKg = pesoKg;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }
}