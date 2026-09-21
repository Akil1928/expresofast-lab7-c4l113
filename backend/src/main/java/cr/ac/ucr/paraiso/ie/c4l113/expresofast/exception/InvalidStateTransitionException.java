package cr.ac.ucr.paraiso.ie.c4l113.expresofast.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String mensaje) {
        super(mensaje);
    }
}