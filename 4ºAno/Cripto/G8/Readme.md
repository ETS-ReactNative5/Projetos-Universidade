Neste guião foram desenvolvidos scripts de cliente/servidor baseados no guião anterior.

Neste caso, foram implementadas keystores para serem extraídos a chave privada de RSA e um certificado que contem a chave pública de RSA, lida por quem o recebe.

**Servidor (Server.py)**:

Arranque:

* Gera chave privada DH (X);
* Gera chave pública DH (gX) baseada em X;
* Carrega certificado (CertA) e chave privada RSA (rX) a partir da sua *keystore*;
* Carrega certificado CA;

C = 0:

* Recebe chave pública DH do cliente (gY);
* Gera chave partilhada (gY.X = gX.Y);
* Gera assinatura (SigA(gX + gY)) através de rX;
* Envia gX + sX + CertA;

C = 1:

* Recebe assinatura e certificado do cliente (SigB + CertB);
* Verifica CertB a partir de CA
* Gera chave pública RSA do cliente (rgY) a partir de CertB;
* Verifica SigB com rgY;
* Envia mensagem encriptada por gX.Y com a confirmação para ser iniciada a conversa;

C > 1:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada por gX.Y;

**Cliente (Client.py)**:

Arranque:
* Gera chave privada DH (Y);
* Gera chave pública DH (gY) baseada em Y;
* Carrega certificado (CertB) e chave privada RSA (rY) a partir da sua *keystore*;
* Envia gY;

C = 0:

* Recebe chave pública DH, assinatura e certificado do servidor (gX + SigA + CertA);
* Verifica CertA a partir de CA
* Gera chave pública RSA do cliente (rgX) a partir de CertA;
* Verifica SigA com rgX;
* Gera chave partilhada (gX.Y);
* Gera assinatura (SigB(gX + gY)) através de rY;
* Envia SigB + CertB;

C > 0:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada por gX.Y;

Os detalhes dos processos estão comentados no código.
