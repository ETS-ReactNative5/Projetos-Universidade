# Script de criação do certificado auto-assinado
# Código baseado em https://rohanc.me/valid-x509-certs-pyopenssl/
import random
from OpenSSL import crypto

p12_password = 'group2' # Palavra-passe da keystore

# Certificado
ca_cert = crypto.X509()
ca_cert.set_version(2)
ca_cert.set_serial_number(random.randint(50000000, 100000000))

# Chave privada de RSA
ca_key_private = crypto.PKey()
ca_key_private.generate_key(crypto.TYPE_RSA, 4096)

# Identificação do titular
ca_subject = ca_cert.get_subject()
ca_subject.CN = 'G2\'s CA'
ca_subject.OU = 'MIETI'
ca_subject.O = 'UMinho'
ca_subject.C = 'PT'

# Identificação da EC (auto-assinado)
ca_cert.set_issuer(ca_subject)

# Chave pública
ca_cert.set_pubkey(ca_key_private)

# Extensões
ca_cert.add_extensions ([
    crypto.X509Extension (
        b'subjectKeyIdentifier',
        False,
        b'hash',
        subject = ca_cert
    ),
])
ca_cert.add_extensions ([
    crypto.X509Extension (
        b'authorityKeyIdentifier',
        False,
        b'keyid:always,issuer',
        issuer = ca_cert
    ),
    crypto.X509Extension (
        b'basicConstraints',
        True,
        b'CA:TRUE'
    ),
    crypto.X509Extension (
        b'keyUsage',
        True,
        b'digitalSignature, keyCertSign, cRLSign'
    ),
])

# Validade do tempo atual até daqui a 1 ano
ca_cert.gmtime_adj_notBefore(0)
ca_cert.gmtime_adj_notAfter(365 * 24 * 60 * 60)

# Assinatura feita pela própria chave privada
ca_cert.sign(ca_key_private, 'sha256')

# Armazenamento do certificado e da chave privada numa keystore
ca_p12 = crypto.PKCS12()
ca_p12.set_certificate(ca_cert)
ca_p12.set_privatekey(ca_key_private)
ca_p12_bytes = ca_p12.export(p12_password)
open('CA/ca.p12', 'wb').write(ca_p12_bytes)