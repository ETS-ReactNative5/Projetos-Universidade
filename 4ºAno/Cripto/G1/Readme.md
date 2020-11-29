Optamos por dois programas:

* encrypter.py - Para o transmissor encriptar um ficheiro (input.txt) que contem uma mensagem e gerar uma chave para encriptar essa mensagem. Ambas a chave a a mensagem encriptada são armazenadas em ficheiros (key.txt e encrypted.txt).
* decrypter.py - Para o recetor desencriptar o ficheiro com a mensagem encriptada recebida através da chave também recebida num ficheiro. A mensagem desencriptada é armazenada num ficheiro (output.txt).

Como meio de ataque à integridade da mensagem, resolvemos alterar o conteúdo do ficheiro com a mensagem encriptada ou com a chave de encriptação, resultando numa falha de desencriptação por parte do programa, como previsto.

Acreditamos que resolvemos este guião com sucesso.
