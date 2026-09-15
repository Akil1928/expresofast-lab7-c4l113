package cr.ac.ucr.paraiso.ie.c4h741.expresofast.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}