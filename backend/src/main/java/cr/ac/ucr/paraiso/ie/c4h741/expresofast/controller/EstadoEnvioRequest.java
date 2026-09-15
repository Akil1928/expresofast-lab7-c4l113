package cr.ac.ucr.paraiso.ie.c4h741.expresofast.controller;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo esperado para PATCH /api/envios/{id}/estado
 * Ejemplo: { "estado": "EN_TRANSITO" }
 */
public class EstadoEnvioRequest {

    @NotBlank
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
