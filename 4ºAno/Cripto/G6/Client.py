# Script do Cliente
# Código baseado em https://docs.python.org/3.6/library/asyncio-stream.html#tcp-echo-client-using-streams
import asyncio
import socket
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac, padding, serialization
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.serialization import load_der_public_key, load_pem_public_key

sv_address = 'localhost'                    # Endereço do servidor
sv_port = 8888                              # Porta do servidor
max_msg_size = 9999
hkdf_info = b'hkdf password'                # Palavra-passe para o HKDF

# Parâmetros de grupo
P = 99494096650139337106186933977618513974146274831566768179581759037259788798151499814653951492724365471316253651463342255785311748602922458795201382445323499931625451272600173180136123245441204133515800495917242011863558721723303661523372572477211620144038809673692512025566673746993593384600667047373692203583
G = 44157404837960328768872680677686802650999163226766694797650810379076416463147265401084491113667624054557335394761604876882446924929840681990106974314935015501571333024773172440352475358750668213444607353872754650805031912866692119819377041901642732455911509867728218394542745330014071040326856846990119719675
pn = dh.DHParameterNumbers(P, G)
parameters = pn.parameters(default_backend())

# Chave privada do cliente
cl_key_private = parameters.generate_private_key()

cl_key_peer_public = cl_key_private.public_key()

# Chave pública do cliente
cl_key_public = cl_key_peer_public.public_bytes (
    encoding = serialization.Encoding.PEM,
    format = serialization.PublicFormat.SubjectPublicKeyInfo
)

class Client:
    def __init__(self, address = None):
        self.address = address
        self.msg_count = -1
        self.key_mac = b''
        self.key_enc = b''

    def process(self, msg = b""):
        # Se não receber mensagem (1ª chamada à função)
        if len(msg) <= 0:       
            print('» Sending my public key...')

            # Envio da chave pública do cliente
            return cl_key_public

        else:
            # Contador de mensagens aumenta
            self.msg_count += 1

            # Se for a 1ª mensagem, trata-se da chave pública do servidor
            if self.msg_count == 0:
                print('> Server [{}] sent its public key.'.format(self.msg_count))
                
                # Chave pública do cliente
                sv_key_public = load_pem_public_key (
                    msg,
                    backend = default_backend()
                )
                
                # Chave partilhada (gerada a partir da chave privada do cliente
                #   e da pública do servidor)
                key_shared = cl_key_private.exchange(sv_key_public)

                hkdf = HKDF (
                    algorithm = hashes.SHA256(),
                    length = 64,
                    salt = None,
                    info = hkdf_info,
                    backend = default_backend()
                )
                
                # Derivação da chave partilhada
                key_derived = hkdf.derive(key_shared)

                # Separação da chave derivada em chave de MAC e chave de
                #   encriptação/desencriptação
                self.key_mac = key_derived[:32]
                self.key_enc = key_derived[32:64]

            else:
                # Separação da mensagem recebida (1ºs 32 bytes formam a tag do
                #   MAC, os restantes formam a mensagem encriptada)
                msg_tag = msg[:32]
                msg_enc = msg[32:]

                # Verificação da tag da mensagem
                mac_check = unmac(msg_enc, self.key_mac, msg_tag)

                # Se a integridade da mensagem tiver sido atacada
                if not mac_check:
                    text = '< integrity compromised! >'

                else:
                    # Desencriptação da mensagem
                    msg_dec = decrypt(msg_enc, self.key_enc, self.msg_count)

                    # Se a confidencialidade da mensagem tiver sido atacada
                    if not msg_dec:
                        text = '< confidentiality compromised! >'

                    else:
                        # Unpadding da mensagem
                        msg = unpad(msg_dec)
                        text = msg.decode()

                print('> Server [{}]: "{}"'.format(self.msg_count, text))

            print('» Input: ', end = '')

            try:
                # Leitura de mensagem no terminal
                my_msg = input().encode()

                if my_msg == b'nothing':
                    my_msg = b''

            # Se for detetado ^C, a mensagem "lida" é vazia
            except KeyboardInterrupt:
                print('')
                my_msg = b''

            # Padding da mensagem
            msg_pad = pad(my_msg)
            
            # Encriptação da mensagem
            msg_enc = encrypt(msg_pad, self.key_enc, self.msg_count)
            
            # Geração da tag da mensagem
            msg_tag = mac(msg_enc, self.key_mac)

            # Concatenação da tag com a mensagem encriptada
            #   se a introduzida não estiver vazia, caso contrário retorna 'None'
            return msg_tag + msg_enc if len(my_msg) > 0 else None


def encrypt(msg, key, count):
    # Vetor de inicialização é o contador da mensagem
    iv = pad(bytes([count]))

    # Encriptação/decriptação por AES com CBC
    cipher = Cipher (
        algorithms.AES(key),
        modes.CBC(iv),
        backend = default_backend()
    )

    encryptor = cipher.encryptor()
    
    return encryptor.update(msg) + encryptor.finalize()

def decrypt(msg, key, count):
    try:
        iv = pad(bytes([count]))

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
            print('> Server shutdown...')

        writer.close()

    else:
        print('> Server is offline...')

def run_client():
    loop = asyncio.get_event_loop()
    loop.run_until_complete(tcp_echo_client())

run_client()