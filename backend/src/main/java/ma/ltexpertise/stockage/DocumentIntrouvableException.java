package ma.ltexpertise.stockage;

public class DocumentIntrouvableException extends StockageException {

    public DocumentIntrouvableException(String bucket, String cle) {
        super("Document introuvable : " + bucket + "/" + cle);
    }
}
