package ma.ltexpertise.securite;

import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/** Émission des jetons d'accès JWT : sujet = identifiant, revendication {@code role} = rôle applicatif. */
@Service
public class ServiceJetons {

    public static final String REVENDICATION_ROLE = "role";
    static final String EMETTEUR = "lt-expertise";

    private final JwtEncoder encodeur;
    private final ProprietesSecurite proprietes;
    private final Clock horloge;

    public ServiceJetons(JwtEncoder encodeur, ProprietesSecurite proprietes, Clock horloge) {
        this.encodeur = encodeur;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    public String jetonAcces(Utilisateur utilisateur) {
        Instant maintenant = horloge.instant();
        JwtClaimsSet revendications = JwtClaimsSet.builder()
                .issuer(EMETTEUR)
                .subject(utilisateur.getIdentifiant())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(proprietes.dureeJetonAcces()))
                .claim(REVENDICATION_ROLE, utilisateur.getRole().name())
                .build();
        JwsHeader entete = JwsHeader.with(MacAlgorithm.HS256).build();
        return encodeur.encode(JwtEncoderParameters.from(entete, revendications)).getTokenValue();
    }

    public long dureeJetonAccesSecondes() {
        return proprietes.dureeJetonAcces().toSeconds();
    }
}
