#Ficheiro de desencriptação da mensagem recebida
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

def writeDecryptedFile(fileName, text):
    f = open(fileName, 'wb')
    f.write(text)

#Leitura da palavra passe no terminal
pw = getpass.getpass()

#Leitura do ficheiro com a mensagem encriptada e o salt
f = open('encrypted.txt', 'rb')
salt = f.read(16)
encryptedText = f.read()

#Obtenção de chave através da palavra pass e do salt
fernet = getKey(pw, salt)

#Desencriptação da mensagem encriptada
decryptedText = fernet.decrypt(encryptedText)
    
#Armazenamento da mensagem desencriptada num ficheiro
writeDecryptedFile('decrypted.txt', decryptedText)
