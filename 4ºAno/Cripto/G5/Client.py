# Código baseado em https://docs.python.org/3.6/library/asyncio-stream.html#tcp-echo-client-using-streams
import asyncio
import socket
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac, padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

sv_address = '127.0.0.1'                    # Endereço do servidor
sv_port = 8888                              # Porta do servidor
max_msg_size = 9999
key = b'this really is a very secure key'   # Chave de encriptação e mac
iv = b'really random iv'                    # Vetor de inicialização

# Encriptação/decriptação por AES com CBC
cipher = Cipher (
    algorithms.AES(key),
    modes.CBC(iv),
    backend = default_backend()
)

class Client:
    def __init__(self, address = None):
        self.address = address
        self.msg_count = 0

    def process(self, msg = b""):
        # Se não receber mensagem vazia
        if (len(msg) > 0):
            # Contador de mensagens aumenta
            self.msg_count += 1

            # Mensagem recebida é separada (1ºs 32 bytes formam a tag do MAC,
            #   os restantes formam a mensagem encriptada)
            msg_tag = msg[:32]
            msg_enc = msg[32:]

            # Verificação da tag da mensagem
            mac_check = unmac(msg_enc, msg_tag)

            if (not mac_check):
                text = '< integrity compromised! >'

            else:
                # Desencriptação da mensagem
                msg_dec = decrypt(msg_enc)

                # Unpadding da mensagem
                msg = unpad(msg_dec)

                text = msg.decode()

            print('> Server (%d): %r' % (self.msg_count, text))
        
        print('Input: ', end = '')

        try:
            # Leitura de mensagem
            my_msg = input().encode()

            if (my_msg == b'nothing'):
                my_msg = b''

        # Se for detetado ^C, a mensagem "lida" é vazia
        except KeyboardInterrupt:
            print('')
            my_msg = b''

        # Padding da mensagem
        msg_pad = pad(my_msg)
        
        # Encriptação da mensagem
        msg_enc = encrypt(msg_pad)
        
        # Geração da tag da mensagem
        msg_tag = mac(msg_enc)
        
        # Concatenação da tag com a mensagem encriptada
        #   se a introduzida não estiver vazia, caso contrário retorna 'None'
        return (msg_tag + msg_enc if (len(my_msg) > 0) else None)

def encrypt(msg):
    encryptor = cipher.encryptor()
    
    return encryptor.update(msg) + encryptor.finalize()

def decrypt(msg):
    decryptor = cipher.decryptor()
    
    return decryptor.update(msg) + decryptor.finalize()

def mac(msg):
    mac = hmac.HMAC (
        key,
        hashes.SHA256(),
        backend = default_backend()
    )

    mac.update(msg)

    return mac.finalize()

def unmac(msg, tag):
    check = True

    try:
        mac = hmac.HMAC (
            key,
            hashes.SHA256(),
            backend = default_backend()
        )

        mac.update(msg)
        mac.verify(tag)
    
    except:
        check = False
        
    return check


def pad(msg):
    # Padding para blocos de 32 bytes
    padder = padding.PKCS7(128).padder()
    text = padder.update(msg)

    return text + padder.finalize()

def unpad(msg):
    unpadder = padding.PKCS7(128).unpadder()
    text = unpadder.update(msg)

    return text + unpadder.finalize()

@asyncio.coroutine
def tcp_echo_client(loop = None):
    if (loop is None):
        loop = asyncio.get_event_loop()

    print('Connecting to server (\'{}\', {})...'.format(sv_address, sv_port))
    sv_isalive = True

    try:
        reader, writer = yield from asyncio.open_connection(sv_address, sv_port, loop = loop)

    # Se o servidor estiver desligado
    except:
        sv_isalive = False

    if (sv_isalive):
        print('  (type ^C or send nothing to disconnect)\n')

        cl_address = writer.get_extra_info('peername')
        client = Client(cl_address)
        msg = client.process()

        while (msg):
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
            print('Disconnecting...')

        else:
            print('Server shutdown...')

        writer.close()

    else:
        print('Server is offline...')

def run_client():
    loop = asyncio.get_event_loop()
    loop.run_until_complete(tcp_echo_client())

run_client()