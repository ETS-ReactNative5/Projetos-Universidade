#Ficheiro de encriptação da mensagem a enviar
from cryptography.fernet import Fernet

key = Fernet.generate_key() #Geração da chave de encriptação
fernet = Fernet(key)

with open('input.txt', 'rb') as f:
    input_ = f.read()       #Leitura do ficheiro de origem

with open('key.txt', 'wb') as f:
    f.write(key)            #Armazenamento da chave de encriptação

encrypted = fernet.encrypt(input_) #Encriptação da mensagem do ficheiro de origem

with open('encrypted.txt', 'wb') as f:
    f.write(encrypted)      #Armazenamento da mensagem encriptada