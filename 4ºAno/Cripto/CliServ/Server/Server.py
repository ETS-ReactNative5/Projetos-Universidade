# Script do servidor
# Código baseado em https://docs.python.org/3.6/library/asyncio-stream.html#tcp-echo-client-using-streams
import asyncio, os
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac, padding, serialization
from cryptography.hazmat.primitives.asymmetric import dh, rsa, padding as apadding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from OpenSSL import crypto

cl_id = 0                                   # ID de cada cliente
sv_address = 'localhost'                    # Endereço do servidor
sv_port = 8888                              # Porta do servidor
max_msg_size = 9999
p12_password = 'group2'                     # Palavra-passe da keystore
hkdf_info = b'hkdf password'                # Palavra-passe para o HKDF

# Parâmetros de grupo de DH
P = 99494096650139337106186933977618513974146274831566768179581759037259788798151499814653951492724365471316253651463342255785311748602922458795201382445323499931625451272600173180136123245441204133515800495917242011863558721723303661523372572477211620144038809673692512025566673746993593384600667047373692203583
G = 44157404837960328768872680677686802650999163226766694797650810379076416463147265401084491113667624054557335394761604876882446924929840681990106974314935015501571333024773172440352475358750668213444607353872754650805031912866692119819377041901642732455911509867728218394542745330014071040326856846990119719675
pn = dh.DHParameterNumbers(P, G)
parameters = pn.parameters(default_backend())

# Chave privada de DH do servidor
sv_key_private_dh = parameters.generate_private_key()

# Chave pública de DH do servidor
sv_key_public_dh = sv_key_private_dh.public_key()
sv_key_public_dh_bytes = sv_key_public_dh.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

# Keystore do servidor
sv_p12_bytes = open('Server/server.p12', 'rb').read()
sv_p12 = crypto.load_pkcs12(sv_p12_bytes, p12_password)

# Certificado do servidor
sv_cert_ossl = sv_p12.get_certificate()
sv_cert_bytes = crypto.dump_certificate(crypto.FILETYPE_PEM, sv_cert_ossl)
sv_cert = crypto.X509.to_cryptography(sv_cert_ossl)

# Chave privada de RSA do servidor
sv_key_private_ossl = sv_p12.get_privatekey()
sv_key_private_rsa_bytes = crypto.dump_privatekey(crypto.FILETYPE_PEM, sv_key_private_ossl)
sv_key_private_rsa = serialization.load_pem_private_key (
    sv_key_private_rsa_bytes,
    password = None,
    backend = default_backend()
)

# Keystore do CA
ca_p12_bytes = open('CA/ca.p12', 'rb').read()
ca_p12 = crypto.load_pkcs12(ca_p12_bytes, p12_password)

# Certificado CA
store = crypto.X509Store()
ca_cert = ca_p12.get_certificate()
store.add_cert(ca_cert)

class Connection(object):
    def __init__(self, id, address = None):
        self.id = id
        self.address = address
        self.msg_count = -1
        self.key_mac = b''
        self.key_enc = b''
        self.sig_message = b''

    def process(self, msg):
        # Contador de mensagens aumenta
        self.msg_count += 1

        # Se receber mensagem vazia, assume que o cliente se desconectou ou teve um
        #   problema com a mensagem recebida (assinatura inválida, confidencialidade
        #   comprometida, etc.)
        if len(msg) <= 0:
            return None
        
        # Se receber a 1ª mensagem, trata-se da chave pública de DH do cliente
        elif self.msg_count == 0:
            print('> Client #{} [{}] sent its public key.'.format(self.id, self.msg_count))

            # Chave pública de DH do cliente
            cl_key_public_dh_bytes = msg
            cl_key_public_dh = load_pem_public_key (
                cl_key_public_dh_bytes,
                backend = default_backend()
            )

            # Chave partilhada (gerada a partir da chave privada de DH do servidor e da
            #   pública do cliente)
            key_shared = sv_key_private_dh.exchange(cl_key_public_dh)
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
            self.key_enc = key_derived[32:]

            # Mensagem a assinar, constituida por ambas as chaves públicas de DH
            self.sig_message = sv_key_public_dh_bytes + cl_key_public_dh_bytes
            
            # Assinatura do servidor assinada pela sua chave privade de RSA
            sv_signature = sv_key_private_rsa.sign (
                self.sig_message,
                apadding.PSS (
                    mgf = apadding.MGF1(hashes.SHA256()),
                    salt_length = apadding.PSS.MAX_LENGTH
                ),
                hashes.SHA256()
            )
            
            print('» Sending my public key, signature and certificate #{}...'.format(self.id))

            # Envio da chave pública de DH, da assinatura e do certificado do servidor
            return sv_key_public_dh_bytes + sv_signature + sv_cert_bytes

        # Se receber a 2ª mensagem, trata-se da assinatura e da primeria mensagem
        #   encriptada pelo cliente
        if self.msg_count == 1:
            print('> Client #{} [{}] sent its signature and certificate.'.format(self.id, self.msg_count))

            # Os primeiros 512 bytes estão reservados para a assinatura do cliente
            cl_signature = msg[:512]
            cl_cert_bytes = msg[512:]

            # Validação do certificado do cliente
            try:
                cl_cert_ossl = crypto.load_certificate(crypto.FILETYPE_PEM, cl_cert_bytes)
                cl_cert = crypto.X509.to_cryptography(cl_cert_ossl)
                cl_storectx = crypto.X509StoreContext(store, cl_cert_ossl)
                crypto.X509StoreContext.verify_certificate(cl_storectx)

                print('» Client #{}\'s certificate accepted!'.format(self.id))

            except:
                print('» Client #{}\'s certificate denied!'.format(self.id))

                return None

            cl_cert_subject = cl_cert.subject.rfc4514_string()
            cl_cert_issuer = cl_cert.issuer.rfc4514_string()
            
            print('  (Subject: ' + cl_cert_subject + ')')
            print('  (Issuer:  ' + cl_cert_issuer + ')')

            # Chave pública de RSA do cliente
            cl_key_public_rsa = cl_cert.public_key()
            
            # Verificação da assinatura do cliente através da sua chave pública de RSA
            try:                
                cl_key_public_rsa.verify (
                    cl_signature,
                    self.sig_message,
                    apadding.PSS (
                        mgf = apadding.MGF1(hashes.SHA256()),
                        salt_length = apadding.PSS.MAX_LENGTH
                    ),
                    hashes.SHA256()
                )

                print('» Client #{}\'s signature accepted!'.format(self.id))

            except:
                print('» Client #{}\'s signature denied!'.format(self.id))

                return None

            my_text = 'Welcome to the chat!'

        # A partir da 2ª mensagem, trata-se de uma mensagem encriptada pelo cliente
        elif self.msg_count >= 2:
            # Separação da mensagem recebida (1ºs 32 bytes formam a tag do MAC, os
            #   restantes formam a mensagem encriptada)
            msg_tag = msg[:32]
            msg_enc = msg[32:]

            # Verificação da tag da mensagem
            mac_check = unmac(msg_enc, self.key_mac, msg_tag)

            # Se a integridade da mensagem tiver sido atacada
            if not mac_check:
                print('» Client #{}\'s message\'s integrity compromised!'.format(self.id))

                return None

            else:
                # Desencriptação da mensagem
                msg_dec = decrypt(msg_enc, self.key_enc)

                # Se a confidencialidade da mensagem tiver sido atacada
                if not msg_dec:
                    print('» Client #{}\'s message\'s confidentiality compromised!'.format(self.id))

                    return None

                else:
                    # Unpadding da mensagem
                    msg = unpad(msg_dec)
                    text = msg.decode()

            print('> Client #{} [{}]: "{}"'.format(self.id, self.msg_count, text))

            # Transformação das letras da mensagem para maiúsculas
            my_text = text.upper()

        my_msg = my_text.encode()
        
        print('» Input #{}: {}'.format(self.id, my_text))

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
def handle_echo(reader, writer):
    global cl_id
    cl_id += 1
    cl_address = writer.get_extra_info('peername')
    connection = Connection(cl_id, cl_address)
    
    print("> Client #{} joined the server {}.".format(connection.id, cl_address))
                
    while True:
        data = yield from reader.read(max_msg_size)

        if not data:
            continue

        if data[:1] == b'\n':
            break
        
        data = connection.process(data)

        if not data:
            break

        writer.write(data)
        yield from writer.drain()

    print("> Client #{} left the server {}.".format(connection.id, cl_address))
    
    writer.close()

def sv_run():
    loop = asyncio.get_event_loop()
    coro = asyncio.start_server(handle_echo, sv_address, sv_port, loop = loop)
    server = loop.run_until_complete(coro)

    print('» Initializing server {}...'.format(server.sockets[0].getsockname()))
    print('  (type ^C to shut it down)\n')

    # Servidor corre até ser detetado ^C
    try:
        loop.run_forever()

    except KeyboardInterrupt:
        pass

    # Fechar servidor
    server.close()
    loop.run_until_complete(server.wait_closed())
    loop.close()

    print('\n» Shutting down...')

sv_run()