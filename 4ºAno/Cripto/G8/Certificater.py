#from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from OpenSSL import crypto

sv_password = b'1234'
cl_password = b'1234'

sv_p12_bytes = open("Servidor.p12", 'rb').read()
sv_p12 = crypto.load_pkcs12(sv_p12_bytes, sv_password)

cl_p12_bytes = open("Cliente1.p12", 'rb').read()
cl_p12 = crypto.load_pkcs12(cl_p12_bytes, cl_password)

sv_cert = sv_p12.get_certificate()
sv_cert_bytes = crypto.dump_certificate(crypto.FILETYPE_PEM, sv_cert)

cl_cert = cl_p12.get_certificate()
cl_cert_bytes = crypto.dump_certificate(crypto.FILETYPE_PEM, cl_cert)

sv_cert = crypto.load_certificate(crypto.FILETYPE_PEM, sv_cert_bytes)

sv_key_private_rsa = sv_p12.get_privatekey()
sv_key_private_rsa_bytes = crypto.dump_privatekey(crypto.FILETYPE_PEM, sv_key_private_rsa)

cl_key_private_rsa = cl_p12.get_privatekey()
cl_key_private_rsa_bytes = crypto.dump_privatekey(crypto.FILETYPE_PEM, cl_key_private_rsa)

store = crypto.X509Store()
ca_certificate_bytes = open('CA.cer', 'rb').read()
ca_certificate = crypto.load_certificate(crypto.FILETYPE_ASN1, ca_certificate_bytes)
store.add_cert(ca_certificate)

sv_storectx = crypto.X509StoreContext(store, sv_cert)
crypto.X509StoreContext.verify_certificate(sv_storectx)

cl_storectx = crypto.X509StoreContext(store, cl_cert)
crypto.X509StoreContext.verify_certificate(cl_storectx)

sv_cert_crypto = crypto.X509.to_cryptography(sv_cert)
#sv_cert_crypto = x509.load_pem_x509_certificate(sv_cert_bytes, default_backend())

cl_cert_crypto = crypto.X509.to_cryptography(cl_cert)

sv_key_public_rsa = sv_cert_crypto.public_key()
sv_key_public_rsa_bytes = sv_key_public_rsa.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

cl_key_public_rsa = cl_cert_crypto.public_key()
cl_key_public_rsa_bytes = cl_key_public_rsa.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

sv_key_private_rsa2 = serialization.load_pem_private_key (
    sv_key_private_rsa_bytes,
    password = None,
    backend = default_backend()
)
sv_key_public_rsa2 = sv_key_private_rsa2.public_key()
sv_key_public_rsa_bytes2 = sv_key_public_rsa.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

print('Server stuff:')
print(len(sv_key_public_rsa_bytes2))

print('Client stuff:')
print(cl_cert_bytes)