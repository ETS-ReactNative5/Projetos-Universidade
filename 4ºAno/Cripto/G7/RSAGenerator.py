# Script de geração de chaves de RSA
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

# Chave privada de RSA do servidor
sv_key_private_rsa = rsa.generate_private_key (
    public_exponent = 65537,
    key_size = 2048,
    backend = default_backend()
)

sv_key_private_rsa_bytes = sv_key_private_rsa.private_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PrivateFormat.PKCS8,
    encryption_algorithm = serialization.NoEncryption()
)

# Armazenamento da chave privada de RSA do servidor
f = open('sv_key_private_rsa.txt', 'wb')
f.write(sv_key_private_rsa_bytes)
f.close()

# Chave pública de RSA do servidor
sv_key_public_rsa = sv_key_private_rsa.public_key()

sv_key_public_rsa_bytes = sv_key_public_rsa.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

# Armazenamento da chave pública de RSA do servidor
f = open('sv_key_public_rsa.txt', 'wb')
f.write(sv_key_public_rsa_bytes)
f.close()

# Chave privada de RSA do cliente
cl_key_private_rsa = rsa.generate_private_key (
    public_exponent = 65537,
    key_size = 2048,
    backend = default_backend()
)

cl_key_private_rsa_bytes = cl_key_private_rsa.private_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PrivateFormat.PKCS8,
    encryption_algorithm = serialization.NoEncryption()
)

# Armazenamento da chave privada de RSA do cliente
f = open('cl_key_private_rsa.txt', 'wb')
f.write(cl_key_private_rsa_bytes)
f.close()

# Chave pública de RSA do cliente
cl_key_public_rsa = cl_key_private_rsa.public_key()

cl_key_public_rsa_bytes = cl_key_public_rsa.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

# Armazenamento da chave pública de RSA do cliente
f = open('cl_key_public_rsa.txt', 'wb')
f.write(cl_key_public_rsa_bytes)
f.close()