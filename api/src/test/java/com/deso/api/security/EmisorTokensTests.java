package com.deso.api.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class EmisorTokensTests {

    private static final String EMISOR = "deso-api";

    private String rutaPem;
    private ClaveFirma claveFirma;
    private EmisorTokens emisorTokens;

    @BeforeEach
    void preparar() throws Exception {
        rutaPem = Path.of(Objects.requireNonNull(
                EmisorTokensTests.class.getResource("/jwt-test-private.pem")).toURI()).toString();
        PropiedadesAuth propiedades = propiedadesCon(rutaPem, EMISOR);
        claveFirma = new ClaveFirma(propiedades);
        emisorTokens = new EmisorTokens(claveFirma, propiedades);
    }

    @Test
    void leerAccessDevuelveSujetoNumericoYRolDeUnTokenVigente() {
        String token = emisorTokens.emitirAccess("42", "CONSULTOR", Instant.now().plusSeconds(300));

        assertThat(emisorTokens.leerAccess(token)).hasValueSatisfying(datos -> {
            assertThat(datos.sujetoId()).isEqualTo(42L);
            assertThat(datos.sujeto()).isEqualTo("42");
            assertThat(datos.rol()).isEqualTo("CONSULTOR");
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no-es-un-jwt", "encabezado.cuerpo.firma-invalida"})
    void leerAccessDevuelveVacioAnteTokensNulosBlancosOMalformados(String tokenInvalido) {
        assertThat(emisorTokens.leerAccess(tokenInvalido)).isEmpty();
    }

    @Test
    void leerAccessDevuelveVacioSiElTokenVencio() {
        String vencido = emisorTokens.emitirAccess("42", "CONSULTOR", Instant.now().minusSeconds(5));

        assertThat(emisorTokens.leerAccess(vencido)).isEmpty();
    }

    @Test
    void leerAccessNoAceptaUnRefreshToken() {
        String refresh = emisorTokens.emitirRefresh(9L, 0L, "42", "CONSULTOR", Instant.now().plusSeconds(300));

        assertThat(emisorTokens.leerAccess(refresh)).isEmpty();
    }

    @Test
    void leerAccessDevuelveVacioSiFueFirmadoConOtraClave(@TempDir Path directorioTemporal) throws Exception {
        Path otraClavePem = generarPemRsa(directorioTemporal.resolve("otra-clave.pem"));
        PropiedadesAuth otrasPropiedades = propiedadesCon(otraClavePem.toString(), EMISOR);
        EmisorTokens otroEmisor = new EmisorTokens(new ClaveFirma(otrasPropiedades), otrasPropiedades);

        String tokenDeOtraClave = otroEmisor.emitirAccess("42", "CONSULTOR", Instant.now().plusSeconds(300));

        assertThat(emisorTokens.leerAccess(tokenDeOtraClave)).isEmpty();
    }

    @Test
    void leerAccessDevuelveVacioSiElEmisorNoCoincide() {
        EmisorTokens otroEmisor = new EmisorTokens(claveFirma, propiedadesCon(rutaPem, "otro-emisor"));
        String token = otroEmisor.emitirAccess("42", "CONSULTOR", Instant.now().plusSeconds(300));

        assertThat(emisorTokens.leerAccess(token)).isEmpty();
    }

    @Test
    void leerAccessRechazaAlgoritmosDistintosDeRs256() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(EMISOR)
                .subject("42")
                .claim("rol", "CONSULTOR")
                .claim("tipo", EmisorTokens.TIPO_ACCESS)
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();

        String sinFirma = new PlainJWT(claims).serialize();
        SignedJWT hs256 = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        hs256.sign(new MACSigner("0123456789abcdef0123456789abcdef"));

        assertThat(emisorTokens.leerAccess(sinFirma)).isEmpty();
        assertThat(emisorTokens.leerAccess(hs256.serialize())).isEmpty();
    }

    @Test
    void leerAccessExigeSujetoNumericoYRolPresente() {
        Instant expira = Instant.now().plusSeconds(300);

        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess(null, "CONSULTOR", expira))).isEmpty();
        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess("", "CONSULTOR", expira))).isEmpty();
        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess("ADMIN", "CONSULTOR", expira))).isEmpty();
        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess(" 42 ", "CONSULTOR", expira))).isEmpty();
        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess("42", null, expira))).isEmpty();
        assertThat(emisorTokens.leerAccess(emisorTokens.emitirAccess("42", "   ", expira))).isEmpty();
    }

    private static Path generarPemRsa(Path destino) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(par.getPrivate().getEncoded());
        Files.writeString(destino, "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n");
        return destino;
    }

    private static PropiedadesAuth propiedadesCon(String rutaPem, String emisor) {
        return new PropiedadesAuth(
                new PropiedadesAuth.Jwt(rutaPem, emisor, Duration.ofMinutes(15)),
                new PropiedadesAuth.Sesion(Duration.ofHours(8), Duration.ofHours(2)),
                new PropiedadesAuth.Cookies(false));
    }
}
