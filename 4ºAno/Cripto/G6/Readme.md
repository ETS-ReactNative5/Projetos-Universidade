Neste guião foram desenvolvidos scripts de cliente/servidor baseados no guião anterior. Em vez de encriptação simétrica com chaves hardcoded, foi desenvolvida assimetria com cada lado a gerar uma chave privada e uma pública, sendo esta última partilhada.

A chave usada para processar as mensagens é então derivada a partir da própria privada e da pública do outro. Como as mensagens são identificadas por um incrementador (C), é sabido se é recebida uma chave pública ou uma mensagem.

*Servidor*:
Setup:
* Gera chave privada (X);
* Gera chave pública (gX);
C = 0:
* Recebe chave pública do cliente (gY);
* Gera chave partilhada (gX.Y);
* Envia gX;
C > 0:
* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada com gX.Y;

*Cliente*:
Setup:
* Gera chave privada (Y);
* Gera chave pública (gY);
* Envia gY;
C = 0:
* Recebe chave pública do servidor (gX);
* Gera chave partilhada (gX.Y);
* Envia mensagem encriptada com gX.Y;
C > 0:
* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada com gX.Y;

Os detalhes dos processos estão comentados no código.