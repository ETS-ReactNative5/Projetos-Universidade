Assim como no Guião 1, foram desenvolvidos scripts para encriptar e desencriptar um ficheiro de texto.

Neste guião, em vez de se armazenar a chave num ficheiro, esta é definida por uma palavra-passe escolhida pelo utilizador em "encrypter.py" e verificada em "decrypter.py".

Além disso, é gerada um valor aleatório para o salt que é armazenado e concatenado pela mensagem encriptada em "encrypted.txt", usado juntamente com a palavra-passe para desencriptar a mensagem encriptada.

Como é suposto, se a palavra-passe introduzida na desencriptação for errada, ocorrerá um erro no programa.

Este guião está dividido em duas diretorias que contêm os ficheiros e scripts mencionados, mas encriptam e desencriptam de maneiras distintas: PBKDF2 e Scrypt.

Acreditamos que resolvemos este guião com sucesso.