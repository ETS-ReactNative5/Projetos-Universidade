#Ficheiro de encriptação da mensagem a enviar
import os, getpass, base64
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.backends import default_backend

def getKey(pw, salt):
    kdf = PBKDF2HMAC (
        algorithm = hashes.SHA256(),
        length = 32,
        salt = salt,
        iterations = 100000,
        backend = default_backend()
    )

    key = kdf.derive(pw.encode())
    fernet = Fernet(base64.b64encode(key))
    return fernet

def readInputFile(fileName):
    f = open(fileName, 'rb')
    text = f.read()
    return text

def writeEncryptedFile(fileName, text, salt):
    f = open(fileName, 'wb')
    f.write(salt + text)

#Leitura da palavra passe no terminal
pw = getpass.getpass()

#Obtenção de número aleatório para o salt
salt = os.urandom(16)

#Obtenção de chave através da palavra pass e do salt
fernet = getKey(pw, salt)

#Leitura do ficheiro com a mensagem de origem
inputText = readInputFile('input.txt')      

#Encriptação da mensagem de origem
encryptedText = fernet.encrypt(inputText)

#Armazenamento da mensagem encriptada e do salt num só ficheiro
writeEncryptedFile('encrypted.txt', encryptedText, salt)