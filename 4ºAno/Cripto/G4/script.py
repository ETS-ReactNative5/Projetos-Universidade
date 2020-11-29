import base64, getpass, os, random, sys
from base64 import b64decode
from base64 import b64encode
from Crypto.Cipher import AES
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, hmac
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from hashlib import md5

inputFile0 = 'input0.txt'
inputFile1 = 'input1.txt'
encryptedFile = 'encrypted.txt'
decryptedFile = 'decrypted.txt'

class Adversary:
    def readInputFile(self, fileName):
        f = open(fileName, 'rb')
        text = f.read()
        return text
    
    def guess(self, encOracle, macOracle, text, encryptedGivenText):
        #Escolha aleatória de uma das mensagens para adivinhar
        print(' Adversary | A escolher mensagem para adivinhar..........................', end = '')
        guess = random.randint(0, 1)
        guessedText = text[guess]
        print(' m{}'.format(guess))

        #Encriptação e obtenção da tag da mensagem escolhida
        print(' Adversary | A encriptar mensagem e gerar tag (m{}).......................'.format(guess), end = '')
        encryptedGuessedText = macOracle(guessedText) + encOracle(guessedText)
        print(' ✓')
        
        #Encriptação e obtenção da tag da mensagem escolhida
        print(' Adversary | A comparar mensagem encriptada (m{}) com mensagem recebida...'.format(guess), end = '')
        return (encryptedGuessedText == encryptedGivenText), guess

class Challenger:
    def getKey(self, pw, salt):
        kdf = PBKDF2HMAC (
            algorithm = hashes.SHA512(),
            length = 64,
            salt = salt,
            iterations = 100000,
            backend = default_backend()
        )

        key = kdf.derive(pw.encode())
        return key

    def aesEncrypt(self, text, key):
        cipher = AES.new(key, AES.MODE_ECB)
        encryptedText = cipher.encrypt(text)
        return encryptedText
    
    def chacha20Encrypt(self, text, key, nonce):
        algorithm = algorithms.ChaCha20(key, nonce) 

        cipher = Cipher (
            algorithm,
            mode = None,
            backend = default_backend()
        )

        encryptor = cipher.encryptor()
        encryptedText = encryptor.update(text)
        return encryptedText

    def getTag(self, text, key):
        h = hmac.HMAC (
            key,
            hashes.SHA256(),
            backend = default_backend()
        )

        h.update(text)
        tag = h.finalize()
        return tag

def IND_CPA(C, A):
    #encOracle = lambda text: C.chacha20Encrypt(text, cypherKey, nonce)
    encOracle = lambda text: C.aesEncrypt(text, cypherKey)
    macOracle = lambda text: C.getTag(text, macKey)

    #Leitura da palavra passe no terminal
    pw = getpass.getpass(prompt = 'Challenger | Password: ')

    #Obtenção de número aleatório de 16 bits para o salt
    print('Challenger | A gerar salt................................................', end = '')
    salt = os.urandom(16)
    print(' ✓')

    #Obtenção de número aleatório de 16 bits para o nonce
    #print('Challenger | A gerar nonce...............................................', end = '')
    #nonce = os.urandom(16)
    #print(' ✓')

    #Obtenção de uma chave de 64 bits através da palavra passe e do salt
    print('Challenger | A gerar chaves..............................................', end = '')
    key = C.getKey(pw, salt)
    print(' ✓')

    #Separação da chave em chave de cifra e chave de MAC, ambas de 32 bits
    cypherKey = key[:32]
    macKey = key[-32:]

    #Leitura dos ficheiro com as mensagem de origem
    print(' Adversary | A ler mensagens de ' + inputFile0 + ' (m0) e ' + inputFile1 + ' (m1)........', end = '')
    inputText = [
        A.readInputFile(inputFile0),
        A.readInputFile(inputFile1)
    ]
    print(' ✓')

    #Escolha aleatória de uma das mensagens
    print('Challenger | A escolher mensagem.........................................', end = '')
    choice = random.randint(0, 1)
    chosenText = inputText[choice]
    print(' m{}'.format(choice))

    #Encriptação da mensagem escolhida através da chave de cifra
    print('Challenger | A encriptar mensagem (m{})...................................'.format(choice), end = '')
    #encryptedChosenText = C.chacha20Encrypt(chosenText, cypherKey, nonce)
    encryptedChosenText = C.aesEncrypt(chosenText, cypherKey)
    print(' ✓')

    #Obtenção da tag da mensagem scolhida através da chave de MAC
    print('Challenger | A gerar tag (m{})............................................'.format(choice), end = '')
    encryptedChosenText = C.getTag(chosenText, macKey) + encryptedChosenText
    print(' ✓')

    result, guessed = A.guess(encOracle, macOracle, inputText, encryptedChosenText)
    
    if (result):
        print(' Mensagens iguais ✓')
        print(' Adversary | Mensagem correta:::::::::::::::::::::::::::::::::::::::::::: m{}'.format(guessed))

    else:
        print(' Mensagens diferentes x')
        print(' Adversary | Mensagem correta:::::::::::::::::::::::::::::::::::::::::::: m{}'.format(1 - guessed))

IND_CPA(Challenger(), Adversary())