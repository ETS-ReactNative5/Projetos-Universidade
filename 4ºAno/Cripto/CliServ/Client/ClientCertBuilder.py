# Script de criação do certificado do cliente
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

# Chave privada de RSA do cliente
cl_key_private = crypto.PKey()
cl_key_private.generate_key(crypto.TYPE_RSA, 4096)

# Certificado do cliente
cl_cert = crypto.X509()
cl_cert.set_version(2)
cl_cert.set_serial_number(random.randint(50000000, 100000000))

# Identificação do titular do cliente
cl_subject = cl_cert.get_subject()
cl_subject.CN = 'G2\'s Client'
cl_subject.OU = 'MIETI'
cl_subject.O = 'UMinho'
cl_subject.C = 'PT'

# Identificação da EC (titular de CA)
cl_cert.set_issuer(ca_subject)

# Chave pública
cl_cert.set_pubkey(cl_key_private)

# Extensões
cl_cert.add_extensions ([
    crypto.X509Extension (
        b'basicConstraints',
        False,
        b'CA:FALSE'
    ),
])
cl_cert.add_extensions ([
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
        subject = cl_cert
    ),
])

# Validade do tempo atual até daqui a 5 anos
cl_cert.gmtime_adj_notBefore(0)
cl_cert.gmtime_adj_notAfter(5 * 365 * 24 * 60 * 60)

# Assinatura feita pela chave privada do CA
cl_cert.sign(ca_key_private, 'sha256')

# Armazenamento do certificado e da chave privada numa keystore
cl_p12 = crypto.PKCS12()
cl_p12.set_certificate(cl_cert)
cl_p12.set_privatekey(cl_key_private)
cl_p12_bytes = cl_p12.export(p12_password)
open('Client/client.p12', 'wb').write(cl_p12_bytes)