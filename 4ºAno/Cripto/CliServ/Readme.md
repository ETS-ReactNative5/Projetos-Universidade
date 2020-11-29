Nesta finalização fez-se um melhoramento do guião 8 implementando a criação de certificados.

Não foram feitos especificamente outros melhoramentos em relação aos guiões anteriores pois cada guião foi uma evolução do anterior (tanto para implementar novas funcionalidades como para corrigir problemas anteriores).

Este guião final contém as seguintes funcionalidades:

# Implementações:

### 3 diretorias:

* `CA`, `Server` e `Client`;
* Para realçar a independência dos agentes da conversa, separou-se o conteúdo (*scripts*, *keystores*) que cada um tem acesso. `Server` é acedido pelo servidor, `Client` é acedido pelo cliente e `CA` é acedido por ambos;
* Implementadas na finalização.

### Certificados e *keystores*:

* Cada agente tem acesso a um *script* de criação de certificados e armazenamento dos mesmos (e das chaves privadas de RSA) em *keystores*;
* *Scripts*: `CABuilder.py`, `ServerCertBuilder.py` e `ClientCertBuilder.py`;
* *Keystores*: `ca.p12`, `server.p12` e `client.p12`;
* Criação de certificados e *keystores* e identificação dos certificados recebidos implementada na finalização;
* Envio e verificação de certificados implementados no guião 8.

### Assinaturas:

* Durante o acordo de chaves, é enviada uma assinatura de cada lado assinada pelas respetivas chaves de RSA contendo como mensagem a concatenação de ambas as chaves de DH.
* Implementadas no guião 7;

### Cifras de chave pública:

* Durante o acordo de chaves, cada lado gera uma chave privada de DH e forma uma pública que é enviada para o outro lado;
* A chave usada para encriptar, desencriptar e proteger a integridade das mensagens é derivada a partir da chave privada do próprio lado e da pública do outro;
* A chave gerada (64 bytes) é separada em duas (primeira metade para o MAC, e segunda para a encriptação/desencriptação);
* Implementadas no guião 6.

### Encriptação e MAC:

* Depois do acordo de chaves, para se enviar uma mensagem, esta é encriptada usando a técnica Encrypt then MAC, AES e CBC com IV de 16 bytes aleatórios;
* Encriptação implementada no guião 1;
* Encrypt then MAC implementado no guião 3;
* AES implementado no guião 4;
* CBC implementado no guião 5;
* IV implementado no guião 5 (fixo) e corrigido no guião 7 (aleatório).

### Arquitetura Cliente/Servidor:

* Cada agente tem acesso na sua respetiva categoria a um *script* para participar numa conversa;
* *Scripts*: `Server.py`, `Client.py`;
* Implementada no guião 5.

# Instruções de utilização dos *scripts*:

* O terminal deve estar na diretoria `/1920-G2/Guioes/CliServ`;
* Criação do certificado auto-assinado: `python3 CA/CABuilder.py`;
* Criação do certificado do servidor: `python3 Server/ServerCertBuilder.py`;
* Criação do certificado do cliente: `python3 Client/ClientCertBuilder.py`;
* Criação do servidor: `python3 Server/Server.py`;
* Conexão do cliente: `python3 Client/Client.py`.

# Funcionalidades dos *scripts*:

## Criação do certificado auto-assinado (`CABuilder.py`):

* Cria certificado (CA);
* Gera chave privada (rCA);
* Identifica titular;
* Identifica a EC como o próprio (auto-assinado);
* Gera chave pública (rgCA) a partir de rCA;
* Define extensões e validade;
* Assina certificado com rCA;
* Armazena CA e rCA em *keystore*.

## Criação do certificado do servidor (`ServerCertBuilder.py`) e do cliente (`ClientCertBuilder.py`):

* Carrega certificado CA e chave privada (rCA) a partir da sua *keystore*;
* Cria certificado (CertA);
* Gera chave privada (rA);
* Identifica titular;
* Identifica a EC como CA;
* Gera chave pública (rgA) a partir de rA;
* Define extensões e validade;
* Assina certificado com rCA;
* Armazena CertA e rA em *keystore*.

## Servidor (`Server.py`):

### Arranque:

* Gera chave privada DH (X);
* Gera chave pública DH (gX) baseada em X;
* Carrega certificado (CertA) e chave privada RSA (rX) a partir da sua *keystore*;
* Carrega certificado CA a partir da sua *keystore*.

### 1ª mensagem recebida:

* Recebe chave pública DH do cliente (gY);
* Gera chave partilhada (gY.X = gX.Y);
* Gera assinatura (SigA(gX + gY)) através de rX;
* Envia gX + sX + CertA.

### 2ª mensagem recebida:

* Recebe assinatura e certificado do cliente (SigB + CertB);
* Verifica CertB a partir de CA
* Gera chave pública RSA do cliente (rgY) a partir de CertB;
* Verifica SigB com rgY;
* Envia mensagem encriptada por gX.Y com a confirmação para ser iniciada a conversa.

### Restantes mensagens:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada por gX.Y.

## Cliente (`Client.py`):

### Arranque:

* Gera chave privada DH (Y);
* Gera chave pública DH (gY) baseada em Y;
* Carrega certificado (CertB) e chave privada RSA (rY) a partir da sua *keystore*;
* Carrega certificado CA a partir da sua *keystore*;
* Envia gY.

### 1ª mensagem recebida:

* Recebe chave pública DH, assinatura e certificado do servidor (gX + SigA + CertA);
* Verifica CertA a partir de CA
* Gera chave pública RSA do cliente (rgX) a partir de CertA;
* Verifica SigA com rgX;
* Gera chave partilhada (gX.Y);
* Gera assinatura (SigB(gX + gY)) através de rY;
* Envia SigB + CertB.

### Restantes mensagens:

* Recebe mensagem encriptada;
* Desencripta mensagem com gX.Y;
* Envia mensagem encriptada por gX.Y.

Os detalhes dos processos estão comentados no código.

Palavra-passe das *keystores*: `group2`.