package com.alphawallet.attestation.ticket;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import com.alphawallet.attestation.core.ASNEncodable;
import com.alphawallet.attestation.core.AttestationCrypto;
import com.alphawallet.attestation.core.SignatureUtility;
import com.alphawallet.attestation.core.Validateable;
import com.alphawallet.attestation.core.Verifiable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class Ticket implements ASNEncodable, Verifiable, Validateable {
  // TODO we need details on this
  public enum TicketClass {
    REGULAR(0),
    VIP(1),
    SPEAKER(2),
    STAFF(3);
    private final int value;

    TicketClass(final int newValue) {
      value = newValue;
    }

    public int getValue() { return value; }
  }

  private final BigInteger ticketId;
  private final TicketClass ticketClass;
  private final int conferenceId;
  private final byte[] riddle;
  private final AlgorithmIdentifier algorithm;
  private final byte[] signature;

  private final byte[] encoded;

  /**
   *
   * @param mail The mail address of the recipient
   * @param ticketId The Id of the ticket
   * @param ticketClass The type of this ticket
   * @param conferenceId The id of the conference for which the ticket should be used
   * @param keys The keys used to sign the cheque
   * @param secret the secret that must be known to cash the cheque
   */
  public Ticket(String mail, BigInteger ticketId, TicketClass ticketClass, int conferenceId,
      AsymmetricCipherKeyPair keys, BigInteger secret ) {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    this.ticketId = ticketId;
    this.ticketClass = ticketClass;
    this.conferenceId = conferenceId;
    this.riddle = crypto.makeRiddle(mail, AttestationType.EMAIL, secret);
    try {
      SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(
          keys.getPublic());
      this.algorithm = spki.getAlgorithm();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ASN1Sequence asn1Tic = makeTicket();
    try {
      this.signature = SignatureUtility.sign(asn1Tic.getEncoded(), keys.getPrivate());
      this.encoded = encodeSignedTicket(asn1Tic, algorithm, signature);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("Public and private keys are incorrect");
    }
  }

  public Ticket(BigInteger ticketId, TicketClass ticketClass, int conferenceId, byte[] riddle,
      AlgorithmIdentifier algorithm, byte[] signature) {
    this.ticketId = ticketId;
    this.ticketClass = ticketClass;
    this.conferenceId = conferenceId;
    this.riddle = riddle;
    this.algorithm = algorithm;
    this.signature = signature;
    ASN1Sequence ticket = makeTicket();
    try {
      this.encoded = encodeSignedTicket(ticket, this.algorithm, this.signature);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("Signature is invalid");
    }
  }



  private ASN1Sequence makeTicket() {
    ASN1EncodableVector ticket = new ASN1EncodableVector();
    ticket.add(new ASN1Integer(ticketId));
    ticket.add(new ASN1Integer(ticketClass.getValue()));
    ticket.add(new ASN1Integer(conferenceId));
    ticket.add(new DEROctetString(riddle));
    return new DERSequence(ticket);
  }

  private byte[] encodeSignedTicket(ASN1Sequence ticket, AlgorithmIdentifier algorithm, byte[] signature) throws IOException {
    ASN1EncodableVector signedTicket = new ASN1EncodableVector();
    signedTicket.add(ticket);
    signedTicket.add(algorithm);
    signedTicket.add(new DERBitString(signature));
    return new DERSequence(signedTicket).getEncoded();
  }

  @Override
  public byte[] getDerEncoding() throws InvalidObjectException {
    return new byte[0];
  }

  @Override
  public boolean checkValidity() {
    return false;
  }

  @Override
  public boolean verify() {
    return false;
  }
}
