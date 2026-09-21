package cr.ac.ucr.paraiso.ie.c4l113.expresofast.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}