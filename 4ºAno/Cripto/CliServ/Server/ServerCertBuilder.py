# Script de criação do certificado do servidor
# Código baseado em https://rohanc.me/valid-x509-certs-pyopenssl/
from OpenSSL import crypto
import random

p12_password = 'group2' # Palavra-passe da keystore

# Keystore do CA
ca_p12_bytes = open('CA/ca.p12', 'rb').read()
ca_p12 = crypto.load_pkcs12(ca_p12_bytes, p12_password)

# Certificado CA
ca_cert = ca_p12.get_certificate()
ca_key_private = ca_p12.get_privatekey()

# Titular de CA
ca_subject = ca_cert.get_subject()

# Chave privada de RSA do servidor
sv_key_private = crypto.PKey()
sv_key_private.generate_key(crypto.TYPE_RSA, 4096)

# Certificado do servidor
sv_cert = crypto.X509()
sv_cert.set_version(2)
sv_cert.set_serial_number(random.randint(50000000, 100000000))

# Identificação do titular do servidor
sv_subject = sv_cert.get_subject()
sv_subject.CN = 'G2\'s Server'
sv_subject.OU = 'MIETI'
sv_subject.O = 'UMinho'
sv_subject.C = 'PT'

# Identificação da EC (titular de CA)
sv_cert.set_issuer(ca_subject)

# Chave pública
sv_cert.set_pubkey(sv_key_private)

# Extensões
sv_cert.add_extensions ([
    crypto.X509Extension (
        b'basicConstraints',
        False,
        b'CA:FALSE'
    ),
])
sv_cert.add_extensions ([
    crypto.X509Extension (
        b'authorityKeyIdentifier',
        False,
        b'keyid',
        issuer = ca_cert
    ),
    crypto.X509Extension (
        b'extendedKeyUsage',
        False,
        b'serverAuth'
    ),
    crypto.X509Extension (
        b'keyUsage',
        True,
        b'digitalSignature, keyEncipherment'
    ),
    crypto.X509Extension (
        b'subjectKeyIdentifier',
        False,
        b'hash',
        subject = sv_cert
    ),
])

# Validade do tempo atual até daqui a 5 anos
sv_cert.gmtime_adj_notBefore(0)
sv_cert.gmtime_adj_notAfter(5 * 365 * 24 * 60 * 60)

# Assinatura feita pela chave privada do CA
sv_cert.sign(ca_key_private, 'sha256')

# Armazenamento do certificado e da chave privada numa keystore
sv_p12 = crypto.PKCS12()
sv_p12.set_certificate(sv_cert)
sv_p12.set_privatekey(sv_key_private)
sv_p12_bytes = sv_p12.export(p12_password)
open('Server/server.p12', 'wb').write(sv_p12_bytes)