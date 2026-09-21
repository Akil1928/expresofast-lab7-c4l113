package cr.ac.ucr.paraiso.ie.c4l113.expresofast.business;

/**
 * Excepcion para reglas de negocio incumplidas
 * (ej: peso del envio mayor a la capacidad del vehiculo).
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
