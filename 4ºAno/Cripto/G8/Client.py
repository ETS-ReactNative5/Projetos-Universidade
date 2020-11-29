# Script do Cliente
# Código baseado em https://docs.python.org/3.6/library/asyncio-stream.html#tcp-echo-client-using-streams
import asyncio, os, socket
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac, padding, serialization
from cryptography.hazmat.primitives.asymmetric import dh, rsa, padding as apadding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.serialization import load_der_public_key, load_pem_public_key
from OpenSSL import crypto

sv_address = 'localhost'                    # Endereço do servidor
sv_port = 8888                              # Porta do servidor
max_msg_size = 9999
p12_password = b'1234'                      # Palavra-passe da keystore
hkdf_info = b'hkdf password'                # Palavra-passe para o HKDF

# Parâmetros de grupo de DH
P = 99494096650139337106186933977618513974146274831566768179581759037259788798151499814653951492724365471316253651463342255785311748602922458795201382445323499931625451272600173180136123245441204133515800495917242011863558721723303661523372572477211620144038809673692512025566673746993593384600667047373692203583
G = 44157404837960328768872680677686802650999163226766694797650810379076416463147265401084491113667624054557335394761604876882446924929840681990106974314935015501571333024773172440352475358750668213444607353872754650805031912866692119819377041901642732455911509867728218394542745330014071040326856846990119719675
pn = dh.DHParameterNumbers(P, G)
parameters = pn.parameters(default_backend())

# Chave privada de DH do cliente
cl_key_private_dh = parameters.generate_private_key()

# Chave pública de DH do cliente
cl_key_public_dh = cl_key_private_dh.public_key()
cl_key_public_dh_bytes = cl_key_public_dh.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

# Keystore do cliente
cl_p12_bytes = open("Cliente1.p12", 'rb').read()
cl_p12 = crypto.load_pkcs12(cl_p12_bytes, p12_password)

# Certificado do cliente
cl_cert_ossl = cl_p12.get_certificate()
cl_cert_bytes = crypto.dump_certificate(crypto.FILETYPE_PEM, cl_cert_ossl)
cl_cert = crypto.X509.to_cryptography(cl_cert_ossl)

# Chave privada de RSA do cliente
cl_key_private_ossl = cl_p12.get_privatekey()
cl_key_private_rsa_bytes = crypto.dump_privatekey(crypto.FILETYPE_PEM, cl_key_private_ossl)
cl_key_private_rsa = serialization.load_pem_private_key (
    cl_key_private_rsa_bytes,
    password = None,
    backend = default_backend()
)

# Certificado CA
store = crypto.X509Store()
ca_cert_bytes = open('CA.cer', 'rb').read()
ca_cert = crypto.load_certificate(crypto.FILETYPE_ASN1, ca_cert_bytes)
store.add_cert(ca_cert)

class Client:
    def __init__(self, address = None):
        self.address = address
        self.msg_count = -2
        self.key_mac = b''
        self.key_enc = b''
        self.sign_msg = b''

    def process(self, msg = b""):
        # Contador de mensagens aumenta
        self.msg_count += 1

        # 1ª chamada à função
        if self.msg_count == -1:
            print('» Sending my public key...')

            # Envio da chave pública do cliente
            return cl_key_public_dh_bytes

        # Se receber a 1ª mensagem, trata-se da chave pública de DH, da assinatura e do
        #   certificado do servidor
        elif self.msg_count == 0:
            print('> Server [{}] sent its public key, signature and certificate.'.format(self.msg_count))
            
            # Os primeiros 625 bytes estão reservados para a chave de DH do servidor, os
            #   256 bytes seguintes para a sua assinatura e os restantes para o seu
            #   certificado
            sv_key_public_dh_bytes = msg[:625]
            sv_signature = msg[625:881]
            sv_cert_bytes = msg[881:]

            # Chave pública de DH do servidor
            sv_key_public_dh = load_pem_public_key (
                sv_key_public_dh_bytes,
                backend = default_backend()
            )

            # Validação do certificado do servidor
            try:
                sv_cert_ossl = crypto.load_certificate(crypto.FILETYPE_PEM, sv_cert_bytes)
                sv_cert = crypto.X509.to_cryptography(sv_cert_ossl)
                sv_storectx = crypto.X509StoreContext(store, sv_cert_ossl)
                crypto.X509StoreContext.verify_certificate(sv_storectx)

                print('» Server\'s certificate accepted!')

            except:
                print('» Server\'s certificate denied!')

                return None

            # Mensagem a assinar, constituida por ambas as chaves públicas de DH
            self.sign_msg = sv_key_public_dh_bytes + cl_key_public_dh_bytes

            # Chave pública de RSA do servidor
            sv_key_public_rsa = sv_cert.public_key()

            # Verificação da assinatura do servidor através da sua chave pública de RSA
            try:
                sv_key_public_rsa.verify (
                    sv_signature,
                    self.sign_msg,
                    apadding.PSS (
                        mgf = apadding.MGF1(hashes.SHA256()),
                        salt_length = apadding.PSS.MAX_LENGTH
                    ),
                    hashes.SHA256()
                )

                print('» Server\'s signature accepted!')

            except:
                print('» Server\'s signature denied!')

                return None
            
            # Chave partilhada (gerada a partir da chave privada de DH do cliente e da
            #   pública do servidor)
            key_shared = cl_key_private_dh.exchange(sv_key_public_dh)
            hkdf = HKDF (
                algorithm = hashes.SHA256(),
                length = 64,
                salt = None,
                info = hkdf_info,
                backend = default_backend()
            )
            
            # Derivação da chave partilhada
            key_derived = hkdf.derive(key_shared)

            # Separação da chave derivada em chave de MAC e chave de encriptação e
            #   desencriptação
            self.key_mac = key_derived[:32]
            self.key_enc = key_derived[32:64]

            # Assinatura do cliente assinada pela sua chave privade de RSA
            cl_signature = cl_key_private_rsa.sign (
                self.sign_msg,
                apadding.PSS (
                    mgf = apadding.MGF1(hashes.SHA256()),
                    salt_length = apadding.PSS.MAX_LENGTH
                ),
                hashes.SHA256()
            )

            print('» Sending my signature and certificate...')

            # Envio da assinatura e do certificado do servidor
            return cl_signature + cl_cert_bytes

        # A partir da 2ª mensagem, trata-se de uma mensagem encriptada pelo cliente
        elif self.msg_count >= 1:
            # Separação da mensagem recebida (1ºs 32 bytes formam a tag do
            #   MAC, os restantes formam a mensagem encriptada)
            msg_tag = msg[:32]
            msg_enc = msg[32:]

            # Verificação da tag da mensagem
            mac_check = unmac(msg_enc, self.key_mac, msg_tag)

            # Se a integridade da mensagem tiver sido atacada
            if not mac_check:
                print('» Server\'s message\'s integrity compromised!')
                
                return None

            else:
                # Desencriptação da mensagem
                msg_dec = decrypt(msg_enc, self.key_enc)

                # Se a confidencialidade da mensagem tiver sido atacada
                if not msg_dec:
                    print('» Server\'s message\'s confidentiality compromised!')

                    return None

                else:
                    # Unpadding da mensagem
                    msg = unpad(msg_dec)
                    text = msg.decode()

            print('> Server [{}]: "{}"'.format(self.msg_count, text))

            print('» Input: ', end = '')

            try:
                # Leitura de mensagem no terminal
                my_msg = input().encode()

                if my_msg == b'' or my_msg == b'nothing':
                    return None

            # Se for detetado ^C, a mensagem "lida" é vazia
            except KeyboardInterrupt:
                print('')
                
                return None

            # Padding da mensagem
            msg_pad = pad(my_msg)
            
            # Encriptação da mensagem
            msg_enc = encrypt(msg_pad, self.key_enc)
            
            # Geração da tag da mensagem
            msg_tag = mac(msg_enc, self.key_mac)

            # Concatenação da tag com a mensagem encriptada
            return msg_tag + msg_enc

def encrypt(msg, key):
    # 16 bytes aleatórios para o vetor de inicialização
    iv = os.urandom(16)

    # Encriptação/decriptação por AES com CBC
    cipher = Cipher (
        algorithms.AES(key),
        modes.CBC(iv),
        backend = default_backend()
    )

    encryptor = cipher.encryptor()
    
    # Prefixação do IV à mensagem encriptada
    return iv + encryptor.update(msg) + encryptor.finalize()

def decrypt(msg, key):
    try:
        # Separação do texto em IV e mensagem encriptada
        iv = msg[:16]
        msg = msg[16:]

        cipher = Cipher (
            algorithms.AES(key),
            modes.CBC(iv),
            backend = default_backend()
        )

        decryptor = cipher.decryptor()
        
        return decryptor.update(msg) + decryptor.finalize()
    
    except:
        return None

def mac(msg, key):
    mac = hmac.HMAC (
        key,
        hashes.SHA256(),
        backend = default_backend()
    )
    
    mac.update(msg)

    return mac.finalize()

def unmac(msg, key, tag):    
    try:
        mac = hmac.HMAC (
            key,
            hashes.SHA256(),
            backend = default_backend()
        )

        mac.update(msg)
        mac.verify(tag)
        
        return True
    
    except:
        return False

def pad(msg):
    # Padding para blocos de 16 bytes
    padder = padding.PKCS7(128).padder()
    text = padder.update(msg)

    return text + padder.finalize()

def unpad(msg):
    unpadder = padding.PKCS7(128).unpadder()
    text = unpadder.update(msg)

    return text + unpadder.finalize()

@asyncio.coroutine
def tcp_echo_client(loop = None):
    if loop is None:
        loop = asyncio.get_event_loop()

    print('» Connecting to server (\'{}\', {})...'.format(sv_address, sv_port))
    
    sv_isalive = True

    try:
        reader, writer = yield from asyncio.open_connection(sv_address, sv_port, loop = loop)

    # Se o servidor estiver desligado
    except:
        sv_isalive = False

    if sv_isalive:
        cl_address = writer.get_extra_info('peername')
        client = Client(cl_address)

        print('> Connected {}...'.format(cl_address))
        print('  (type ^C or send nothing to disconnect)\n')
        
        msg = client.process()

        while msg:
            writer.write(msg)
            msg = yield from reader.read(max_msg_size)

            if (msg):
                msg = client.process(msg)

            # Se o servidor estiver desligado
            else:
                sv_isalive = False
                break

        writer.write(b'\n')

        # Se o cliente se desconectar por envio de mensagem vazia ou ^C
        if (sv_isalive):
            print('» Disconnecting...')

        else:
            print('> Disconnecting abruptly...')

        writer.close()

    else:
        print('> Server is offline...')

def run_client():
    loop = asyncio.get_event_loop()
    loop.run_until_complete(tcp_echo_client())

run_client()