# Código baseado em https://docs.python.org/3.6/library/asyncio-stream.html#tcp-echo-client-using-streams
import asyncio
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac, padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

cl_id = 0                                   # ID de cada cliente
sv_address = '127.0.0.1'                    # Endereço do servidor
sv_port = 8888                              # Porta do servidor
max_msg_size = 9999
key = b'this really is a very secure key'   # Chave de encriptação e MAC
iv = b'really random iv'                    # Vetor de inicialização
mac_attacker = False                        # Testador do MAC (= True para atacar a integridade)

# Encriptação/decriptação por AES com CBC
cipher = Cipher (
    algorithms.AES(key),
    modes.CBC(iv),
    backend = default_backend()
)

class Connection(object):
    def __init__(self, id, address = None):
        self.id = id
        self.address = address
        self.msg_count = 0

    def process(self, msg):        
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

            print('> Client #%d: %r' % (self.id, text))

            # Transformação das letras da mensagem para maiúsculas
            my_msg = text.upper().encode()

            # Padding da mensagem
            msg_pad = pad(my_msg)
            
            # Encriptação da mensagem
            msg_enc = encrypt(msg_pad)
            
            # Geração da tag da mensagem
            msg_tag = mac(msg_enc)
            
            # Concatenação da tag com a mensagem encriptada
            return (msg_tag + msg_enc)

        # Se receber mensagem vazia, assume que o cliente se desconectou
        else:
            return none

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
    # Se o testador do MAC estiver 'True', é adicionado um byte à mensagem
    if (mac_attacker):
        msg += b'b'
    
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
def handle_echo(reader, writer):
    global cl_id
    cl_id += 1
    cl_address = writer.get_extra_info('peername')
    connection = Connection(cl_id, cl_address)
    print("> Client #{} joined the server {}.".format(connection.id, cl_address))
    data = yield from reader.read(max_msg_size)

    while True:
        if (not data):
            continue

        if (data[:1] == b'\n'):
            break

        data = connection.process(data)

        if (not data):
            break

        writer.write(data)
        yield from writer.drain()
        data = yield from reader.read(max_msg_size)

    print("> Client #{} left the server {}.".format(connection.id, cl_address))
    writer.close()


def sv_run():
    loop = asyncio.get_event_loop()
    coro = asyncio.start_server(handle_echo, sv_address, sv_port, loop = loop)
    server = loop.run_until_complete(coro)

    print('Initializing server {}...'.format(server.sockets[0].getsockname()))
    print('  (type ^C to shut it down)\n')

    # Servidor corre até ser detetado ^C
    try:
        loop.run_forever()

    except KeyboardInterrupt:
        pass

    # Close the server
    server.close()
    loop.run_until_complete(server.wait_closed())
    sv_isempty = True
    loop.close()
    print('\nShutting down...')

sv_run()