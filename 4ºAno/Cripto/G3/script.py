import base64, getpass, os, sys
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

inputFile = 'input.txt'
encryptedFile = 'encrypted.txt'
decryptedFile = 'decrypted.txt'

def readInputFile(fileName):
    f = open(fileName, 'rb')
    text = f.read()
    return text

def getKey(pw, salt):
    kdf = PBKDF2HMAC (
        algorithm = hashes.SHA512(),
        length = 64,
        salt = salt,
        iterations = 100000,
        backend = default_backend()
    )

    key = kdf.derive(pw.encode())
    return key

def encrypt(text, key, nonce):
    algorithm = algorithms.ChaCha20(key, nonce)

    cipher = Cipher (
        algorithm,
        mode = None,
        backend = default_backend()
    )

    encryptor = cipher.encryptor()
    encryptedText = encryptor.update(text)
    return encryptedText

def decrypt(text, key, nonce):
    algorithm = algorithms.ChaCha20(key, nonce)

    cipher = Cipher (
        algorithm,
        mode = None,
        backend = default_backend()
    )

    decryptor = cipher.decryptor()
    decryptedText = decryptor.update(text)
    return decryptedText

def getTag(text, key):
    h = hmac.HMAC (
        key,
        hashes.SHA256(),
        backend = default_backend()
    )

    h.update(text)
    tag = h.finalize()
    return tag

def checkTag(text, key, tag):
    h = hmac.HMAC (
        key,
        hashes.SHA256(),
        backend = default_backend()
    )

    h.update(text)
    h.verify(tag)

def writeOutputFile(fileName, text):
    f = open(fileName, 'wb')
    f.write(text)

def encryption(flag, pw):
    #Leitura do ficheiro com a mensagem de origem
    print('A ler mensagem de ' + inputFile + '...', end = '')
    inputText = readInputFile(inputFile)
    print(' ✓')

    if (pw == ''):
        #Leitura da palavra passe no terminal
        pw = getpass.getpass()

    #Obtenção de número aleatório de 16 bits para o salt
    print('A gerar salt...', end = '')
    salt = os.urandom(16)
    print(' ✓')

    #Obtenção de número aleatório de 16 bits para o nonce
    print('A gerar nonce...', end = '')
    nonce = os.urandom(16)
    print(' ✓')

    #Obtenção de uma chave de 64 bits através da palavra passe e do salt
    print('A gerar chave...', end = '')
    key = getKey(pw, salt)
    print(' ✓')

    #Separação da chave em chave de cifra e chave de MAC, ambas de 32 bits
    cypherKey = key[:32]
    macKey = key[-32:]

    if (flag == 1):
        #Encriptação da mensagem de origem através da chave de cifra e do nonce
        print('A encriptar mensagem...', end = '')
        encryptedText = encrypt(inputText, cypherKey, nonce)
        print(' ✓')

        #Obtenção da tag através da mensagem de origem e da chave de MAC
        print('A gerar tag...', end = '')
        tag = getTag(inputText, macKey)
        print(' ✓')

        #Prefixação da tag à mensagem encriptada
        encryptedText = tag + encryptedText

    elif (flag == 2):
        #Encriptação da mensagem de origem através da chave de cifra e do nonce
        print('A encriptar mensagem...', end = '')
        encryptedText = encrypt(inputText, cypherKey, nonce)
        print(' ✓')

        #Obtenção da tag através da mensagem encriptada e da chave de MAC
        print('A gerar tag...', end = '')
        tag = getTag(encryptedText, macKey)
        print(' ✓')

        #Prefixação da tag à mensagem encriptada
        encryptedText = tag + encryptedText

    else:
        #Obtenção da tag através da mensagem de origem e da chave de MAC
        print('A gerar tag...', end = '')
        tag = getTag(inputText, macKey)
        print(' ✓')

        #Encriptação da mensagem de origem com a tag através da chave de cifra e do nonce
        print('A encriptar mensagem...', end = '')
        encryptedText = encrypt(tag + inputText, cypherKey, nonce)
        print(' ✓')

    #Armazenamento da mensagem encriptada, o salt, o nonce e a tag num só ficheiro
    print('A armazenar mensagem encriptada em ' + encryptedFile + '...', end = '')
    writeOutputFile(encryptedFile, salt + nonce + encryptedText)
    print(' ✓')

def decryption(flag, pw):
    if (pw == ''):
        #Leitura da palavra passe no terminal
        pw = getpass.getpass()

    #Leitura do ficheiro com a mensagem encriptada, salt, nonce e tag
    print('A abrir ' + encryptedFile + '...', end = '')
    f = open(encryptedFile, 'rb')
    print(' ✓')
    print('A ler salt...', end = '')
    salt = f.read(16)
    print(' ✓')
    print('A ler nonce...', end = '')
    nonce = f.read(16)
    print(' ✓')
    print('A ler tag e mensagem encriptada...', end = '')
    encryptedText = f.read()
    print(' ✓')

    #Obtenção de uma chave de 64 bits através da palavra passe e do salt
    print('A gerar chave...', end = '')
    key = getKey(pw, salt)
    print(' ✓')

    #Separação da chave em chave de cifra e chave de MAC, ambas de 32 bits
    cypherKey = key[:32]
    macKey = key[-32:]

    if (flag == 1):
        tag = encryptedText[:32]
        encryptedText = encryptedText[32:]

        #Desencriptação da mensagem encriptada
        print('A desencriptar mensagem...', end = '')
        decryptedText = decrypt(encryptedText, cypherKey, nonce)
        print(' ✓')

        #Verificação da tag com a mensagem desencriptada
        print('A verificar tag...', end = '')
        checkTag(decryptedText, macKey, tag)
        print(' ✓')

    elif (flag == 2):
        tag = encryptedText[:32]
        encryptedText = encryptedText[32:]
        
        #Desencriptação da mensagem encriptada
        print('A desencriptar mensagem...', end = '')
        decryptedText = decrypt(encryptedText, cypherKey, nonce)
        print(' ✓')

        #Verificação da tag com a mensagem encriptada
        print('A verificar tag...', end = '')
        checkTag(encryptedText, macKey, tag)
        print(' ✓')

    else:
        #Desencriptação da mensagem encriptada
        print('A desencriptar mensagem...', end = '')
        decryptedText = decrypt(encryptedText, cypherKey, nonce)
        print(' ✓')
        
        tag = decryptedText[:32]
        decryptedText = decryptedText[32:]

        #Verificação da tag com a mensagem desencriptada
        print('A verificar tag...', end = '')
        checkTag(decryptedText, macKey, tag)
        print(' ✓')

    #Armazenamento da mensagem desencriptada num ficheiro
    print('A armazenar mensagem desencriptada em ' + decryptedFile + '...', end = '')
    writeOutputFile(decryptedFile, decryptedText)
    print(' ✓')

if (len(sys.argv) > 3):
    mode = str(sys.argv[1])
    flag = str(sys.argv[2])
    pw = str(sys.argv[3])
    default = False

elif (len(sys.argv) > 2):
    mode = str(sys.argv[1])
    flag = str(sys.argv[2])
    pw = ''
    default = False

elif (len(sys.argv) > 1):
    mode = str(sys.argv[1])
    flag = '1'
    pw = ''
    default = True

else:
    mode = '0'
    flag = '0'
    pw = ''
    default = True

if (mode in ['1', '2'] and flag in ['1', '2', '3']):
    if (mode == '1'):
        print('Encriptação | ', end = '')

        if (flag == '1'):
            if (default is False):
                print('Encrypt and MAC')
            
            else:
                print('Encrypt and MAC (default)')

        elif (flag == '2'):
            print('Encrypt then MAC')

        else:
            print('MAC then encrypt')
        
        encryption(flag, pw)

    else:
        print('Desencriptação | ', end = '')
        
        if (flag == '1'):
            if (default is False):
                print('Decrypt then MAC')
            
            else:
                print('Decrypt then MAC (default)')

        elif (flag == '2'):
            print('Decrypt and MAC')

        else:
            print('Decrypt to MAC')
        
        decryption(flag, pw)

else:
    print('Argumentos inválidos.')
    print('Deve ser usado o comando \'python3 ' + sys.argv[0] + ' [MODE] [FLAG (opcional)] [PASSWORD (opcional)]\':')
    print('[MODE] - 1 -> encriptação | 2 -> desencriptação;')
    print('[FLAG] - 1 -> Encrypt and MAC (default) | 2 -> Encrypt then MAC | 3 -> MAC then Encrypt;')
    print('[PASSWORD] - Palavra passe (se não for definida, será pedida durante a execução).')