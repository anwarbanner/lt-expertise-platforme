package ma.ltexpertise.stockage;

/** Échec d'accès au stockage des documents (MinIO indisponible, droits, etc.). */
public class StockageException extends RuntimeException {

    public StockageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StockageException(String message) {
        super(message);
    }
}
