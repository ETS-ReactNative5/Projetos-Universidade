Neste guião foram desenvolvidos scripts de cliente/servidor baseados no guião anterior e um adicional para geração de chaves privadas e públicas de RSA.

Estas chaves servem para assinar (própria chave privada) e confirmar assinaturas (chave pública do outro lado) feitas por cada lado.

**Gerador de chaves RSA (RSAGenerator.py)**:
* Gera chave privada RSA do servidor (rX) e armazena-a;
* Gera chave pública RSA do servidor (rgX) baseada em rX e armazena-a;
* Gera chave privada RSA do cliente (rY) e armazena-a;
* Gera chave pública RSA do cliente (rgY) baseada em rY e armazena-a;

**Servidor (Server.py)**:

Arranque:

* Gera chave privada DH (X);
* Gera chave pública DH (gX) baseada em X;

C = 0:

* Recebe chave pública DH do cliente (gY);
* Gera chave partilhada (gY.X = gX.Y);
* Lê chave pública RSA do servidor (rX);
* Gera assinatura (sX(gX.gY)) através de rX;
* Envia gX + sX;

C = 1:

* Recebe assinatura do cliente (sY) e mensagem encriptada;
* Lê chave pública RSA do cliente (rgY);
* Verifica assinatura com rgY;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada com gX.Y;

C > 1:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada com gX.Y;

**Cliente (Client.py)**:

Arranque:

* Gera chave privada DH (Y);
* Gera chave pública DH (gY) baseada em Y;
* Gera chave privada RSA (rY);
* Gera chave pública RSA (rgY);
* Armazena rgY;
* Envia gY;

C = 0:

* Recebe chave pública DH e assinatura do servidor (gX e sX);
* Lê chave pública RSA do cliente (rgX);
* Verifica assinatura com rgX;
* Gera chave partilhada (gX.Y);
* Lê chave pública RSA do cliente (rY);
* Gera assinatura (sY(gX.gY)) através de rY;
* Envia sY + mensagem encriptada com gX.Y;

C > 0:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada com gX.Y;

Os detalhes dos processos estão comentados no código.
