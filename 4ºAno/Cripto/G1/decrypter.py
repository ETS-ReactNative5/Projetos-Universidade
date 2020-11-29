#Ficheiro de desencriptação da mensagem recebida
from cryptography.fernet import Fernet

with open('key.txt', 'rb') as f:
    key = f.read()          #Leitura da chave armazenada

fernet = Fernet(key)

with open('encrypted.txt', 'rb') as f:
    encrypted = f.read()    #Leitura do ficheiro encriptado

output = fernet.decrypt(encrypted) #Desencriptação da mensagem encriptada

with open('output.txt', 'wb') as f:
    f.write(output)         #Armazenamento da mensagem desencriptada