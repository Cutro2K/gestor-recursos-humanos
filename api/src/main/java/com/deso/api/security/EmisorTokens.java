package com.deso.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y verifica los JWT de sesión (CU 01). Los dos tipos usan la misma clave, así que
 * el claim {@code tipo} se controla siempre: sin eso, un refresh token pasaría por access.
 */
@Component
public class EmisorTokens {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    /** Datos que el refresh token necesita para encontrar y validar su sesión. */
    public record DatosRefresh(long sesionId, long seq, String sujeto, String rol) {}

    private final ClaveFirma claveFirma;
    private final String emisor;

    public EmisorTokens(ClaveFirma claveFirma, PropiedadesAuth propiedades) {
        this.claveFirma = claveFirma;
        this.emisor = propiedades.jwt().emisor();
    }

    public String emitirAccess(String sujeto, String rol, Instant expira) {
        return firmar(new JWTClaimsSet.Builder()
                .subject(sujeto)
                .claim("rol", rol)
                .claim("tipo", TIPO_ACCESS)
                .expirationTime(Date.from(expira)));
    }

    public String emitirRefresh(long sesionId, long seq, String sujeto, String rol, Instant expira) {
        return firmar(new JWTClaimsSet.Builder()
                .subject(sujeto)
                .claim("rol", rol)
                .claim("tipo", TIPO_REFRESH)
                .claim("sesionId", sesionId)
                .claim("seq", seq)
                .expirationTime(Date.from(expira)));
    }

    /**
     * Lee un refresh token. Devuelve vacío si la firma, el emisor, el tipo o los claims no
     * son válidos, o si venció (salvo que {@code aceptarVencido} sea true, que usa el logout
     * para poder borrar la fila aunque la sesión ya haya expirado).
     */
    public Optional<DatosRefresh> leerRefresh(String token, boolean aceptarVencido) {
        return verificar(token, TIPO_REFRESH, aceptarVencido).flatMap(claims -> {
            try {
                Long sesionId = claims.getLongClaim("sesionId");
                Long seq = claims.getLongClaim("seq");
                String rol = claims.getStringClaim("rol");
                if (sesionId == null || seq == null || rol == null || claims.getSubject() == null) {
                    return Optional.empty();
                }
                return Optional.of(new DatosRefresh(sesionId, seq, claims.getSubject(), rol));
            } catch (ParseException e) {
                return Optional.empty();
            }
        });
    }

    private String firmar(JWTClaimsSet.Builder claims) {
        Instant ahora = Instant.now();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(claveFirma.par().getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT jwt = new SignedJWT(header, claims.issuer(emisor).issueTime(Date.from(ahora)).build());
        try {
            jwt.sign(new RSASSASigner(claveFirma.par()));
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de sesión.", e);
        }
        return jwt.serialize();
    }

    private Optional<JWTClaimsSet> verificar(String token, String tipoEsperado, boolean aceptarVencido) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            // Se fija el algoritmo: no se acepta lo que diga el header (p. ej. "none" o HS256).
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new RSASSAVerifier(claveFirma.par().toRSAPublicKey()))) {
                return Optional.empty();
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expira = claims.getExpirationTime();
            boolean valido = emisor.equals(claims.getIssuer())
                    && tipoEsperado.equals(claims.getStringClaim("tipo"))
                    && expira != null
                    && (aceptarVencido || expira.toInstant().isAfter(Instant.now()));
            return valido ? Optional.of(claims) : Optional.empty();
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }
}
