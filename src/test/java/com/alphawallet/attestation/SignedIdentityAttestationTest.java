package com.alphawallet.attestation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphawallet.attestation.core.SignatureUtility;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SignedIdentityAttestationTest {
  private static AsymmetricCipherKeyPair subjectKeys;
  private static AsymmetricCipherKeyPair issuerKeys;
  private static SecureRandom rand;

  @BeforeAll
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());
    subjectKeys = SignatureUtility.constructECKeysWithSmallestY(rand);
    X9ECParameters SECP364R1 = SECNamedCurves.getByName("secp384r1");
    issuerKeys = SignatureUtility.constructECKeys(SECP364R1, rand);
  }

  @Test
  public void testSignAttestation() {
    IdentifierAttestation att = HelperTest.makeUnsignedStandardAtt(subjectKeys.getPublic(), issuerKeys.getPublic(), BigInteger.ONE, "some@mail.com" );
    SignedIdentityAttestation signed = new SignedIdentityAttestation(att, issuerKeys);
    assertTrue(signed.checkValidity());
    assertTrue(signed.verify());
    assertTrue(SignatureUtility.verifySHA256(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
  }

  @Test
  public void testDecoding() throws Exception {
    IdentifierAttestation att = HelperTest.makeUnsignedStandardAtt(subjectKeys.getPublic(), issuerKeys.getPublic(), BigInteger.TEN, "someOther@mail.com" );
    SignedIdentityAttestation signed = new SignedIdentityAttestation(att, issuerKeys);
    assertTrue(SignatureUtility.verifySHA256(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
    byte[] signedEncoded = signed.getDerEncoding();
    SignedIdentityAttestation newSigned = new SignedIdentityAttestation(signedEncoded, issuerKeys.getPublic());
    assertArrayEquals(signed.getDerEncoding(), newSigned.getDerEncoding());
  }

  @Test
  public void invalidAlgorithmParameter() throws Exception {
    IdentifierAttestation att = HelperTest.makeUnsignedStandardAtt(subjectKeys.getPublic(), issuerKeys.getPublic(), BigInteger.TEN, "some@mail.com" );
    SignedIdentityAttestation signed = new SignedIdentityAttestation(att, issuerKeys);
    assertThrows(IllegalArgumentException.class, () ->  new SignedIdentityAttestation(signed.getDerEncoding(), subjectKeys.getPublic()));
  }
  
}
