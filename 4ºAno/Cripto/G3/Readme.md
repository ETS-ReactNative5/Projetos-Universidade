Neste guião, ao contrário dos anteriores, usou-se apenas um ficheiro (script.py) para encriptar e desencriptar, dependendo do 1º argumento inserido na linha de comandos.
O 2º argumento permite escolher o modo de encriptação/desencriptação.

Sintaxe: 'python3 script.py [MODE] [FLAG (opcional)] [PASSWORD (opcional)]':
* [MODE] - 1 -> encriptação | 2 -> desencriptação;
* [FLAG] - 1 -> Encrypt and MAC (default) | 2 -> Encrypt then MAC | 3 -> MAC then Encrypt;
* [PASSWORD] - Palavra passe (se não for definida, será pedida durante a execução).

Exemplo para encriptação por MAC then Encrypt: 'python3 1 3'

Os detalhes do processo de encriptação/desencriptação estão comentados no código.