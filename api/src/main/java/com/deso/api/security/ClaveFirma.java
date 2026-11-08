package com.deso.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Par de claves RSA con el que la API firma los JWT (RS256).
 * La privada se lee de un archivo PEM fuera del repositorio; la pública se deriva de ella
 * y se publica en el JWKS para que el frontend pueda verificar firmas sin poder emitirlas.
 */
@Component
public class ClaveFirma {

    private final RSAKey clave;

    public ClaveFirma(PropiedadesAuth propiedades) {
        this.clave = cargar(Path.of(propiedades.jwt().clavePrivada()));
    }

    /** Par completo (pública + privada), solo para firmar dentro de la API. */
    RSAKey par() {
        return clave;
    }

    /** JWKS con únicamente la clave pública, listo para serializar como JSON. */
    public Map<String, Object> jwksPublico() {
        return new JWKSet(clave.toPublicJWK()).toJSONObject();
    }

    private static RSAKey cargar(Path ruta) {
        String pem;
        try {
            pem = Files.readString(ruta);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la clave privada JWT en '" + ruta.toAbsolutePath()
                    + "'. Generala con scripts/generar-claves-jwt.sh o indicá su ruta en JWT_PRIVATE_KEY_PATH.", e);
        }

        if (!pem.contains("-----BEGIN PRIVATE KEY-----")) {
            throw new IllegalStateException("La clave en '" + ruta.toAbsolutePath()
                    + "' no es un PEM PKCS#8 (debe empezar con -----BEGIN PRIVATE KEY-----).");
        }

        try {
            String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privada = (RSAPrivateCrtKey) fabrica.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));

            // El kid es el thumbprint de la pública: cambia solo si cambia la clave.
            return new RSAKey.Builder(publica)
                    .privateKey(privada)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException | JOSEException e) {
            throw new IllegalStateException("La clave privada JWT en '" + ruta.toAbsolutePath()
                    + "' no es una clave RSA válida.", e);
        }
    }
}
