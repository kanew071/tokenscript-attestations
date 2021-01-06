package com.alphawallet.attestation;

import com.alphawallet.attestation.core.AttestationCrypto;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.math.ec.ECPoint;

public class FullProofOfExponent implements ProofOfExponent {
  private final ECPoint riddle;
  private final ECPoint tPoint;
  private final BigInteger challenge;
  private final byte[] encoding;

  public FullProofOfExponent(ECPoint riddle, ECPoint tPoint, BigInteger challenge) {
    this.riddle = riddle;
    this.tPoint = tPoint;
    this.challenge = challenge;
    this.encoding = makeEncoding(riddle, tPoint, challenge);
  }

  public FullProofOfExponent(byte[] derEncoded) {
    this.encoding = derEncoded;
    try {
      ASN1InputStream input = new ASN1InputStream(derEncoded);
      ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
      int asn1counter = 0;
      ASN1OctetString riddleEnc = ASN1OctetString.getInstance(asn1.getObjectAt(asn1counter++));
      this.riddle = AttestationCrypto.decodePoint(riddleEnc.getOctets());
      ASN1OctetString challengeEnc = ASN1OctetString.getInstance(asn1.getObjectAt(asn1counter++));
      this.challenge = new BigInteger(challengeEnc.getOctets());
      ASN1OctetString tPointEnc = ASN1OctetString.getInstance(asn1.getObjectAt(asn1counter++));
      this.tPoint = AttestationCrypto.decodePoint(tPointEnc.getOctets());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] makeEncoding(ECPoint riddle, ECPoint tPoint, BigInteger challenge) {
    try {
      ASN1EncodableVector res = new ASN1EncodableVector();
      res.add(new DEROctetString(riddle.getEncoded(false)));
      res.add(new DEROctetString(challenge.toByteArray()));
      res.add(new DEROctetString(tPoint.getEncoded(false)));
      return new DERSequence(res).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ECPoint getRiddle() {
    return riddle;
  }

  @Override
  public ECPoint getPoint() {
    return tPoint;
  }

  @Override
  public BigInteger getChallenge() {
    return challenge;
  }

  public UsageProofOfExponent getUsageProofOfExponent() {
    return new UsageProofOfExponent(tPoint, challenge);
  }

  @Override
  public byte[] getDerEncoding() {
    return encoding;
  }

}
