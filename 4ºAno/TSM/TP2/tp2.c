#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#define M_SIZE 1000000
#define EXTRA 500000
#define DICT_SIZE 4096
#define NULL_INT 1000
#define EMPTY -1
enum{
    dictSize = 4096,
};

struct Symbols {
    unsigned char symbol;
    int occurs;
    float prob;
    char code[256];
};struct node {
    unsigned int data;
    struct node* left;
    struct node* right;
};
struct Dictionary{
    int length;
    char letter[128];
};
struct Dictionary dict[4096] = {{0}};
int debug = 0, fileSize = 0, compressedSize = 0, block = 1;
clock_t lzwTime = 0;
int leftover = 0;
int leftoverBits;
int totalSteps = 0;
int saveLast = -1;

void shannonFano(int start,int end, struct Symbols s[256],int step) {
    int first=start;
    int last=end;
    int firstSetSum = s[first].occurs,lastSetSum = s[last].occurs;
 
    if(step>totalSteps) {
        totalSteps = step;
    }
    while(first<(last-1)) {
        while(firstSetSum>=lastSetSum && first<(last-1)) {               //fim
            last--;
            lastSetSum += s[last].occurs;
        }
        while(firstSetSum<lastSetSum && first<(last-1)) {               //inicio
            first++;
            firstSetSum += s[first].occurs;
        }
    }
    if(first==start) {
        s[start].code[step]='0';
    } else if((first-start)>=1) {
        for(int k=start;k<=first;++k) 
            s[k].code[step] = '0';

        shannonFano(start,first,s,step+1);
    }
    if(last==end) {
        s[end].code[step]='1';
    } else if((end-last)>=1) {
        for(int k=last;k<=end;++k) 
            s[k].code[step] = '1';

        shannonFano(last,end,s,step+1);
    }
}

//DECOMPRESSION
void dicionaryInit(){
    for(int i = 0; i < 256;i++){
        dict[i].letter[0] = (char)i;
        dict[i].length = 1;
    }
}
void dictionaryAdd(char* letter, int length, int value) {
    dict[value].length = length;
    memcpy(dict[value].letter,letter,length);
}
char *dictionaryGetLetter(int value) {
    return dict[value].letter;
}
struct node* creatNode(unsigned int data) {
  struct node* node = malloc(sizeof(node)); 
  node->data = data;
  node->left = NULL;
  node->right = NULL;

  return(node);
}
struct node* insert(struct node* node,char *code, unsigned char data) {
    if (*code == '0'){                                  //se o código tiver um 0 (zero)
        *(code++);                                      //avançar posição do código
        if(node == NULL) node = creatNode(NULL_INT);    //caso o nó da árvore seja nulo, criar um nó com valor nulo
        node->left = insert(node->left, code, data);    //como é 0 avançar para a esquerda   
    } 
    else if(*code == '1'){                              //se o código tiver um 1 (um)
        *(code++);
        if(node == NULL) node = creatNode(NULL_INT);
        node->right = insert(node->right, code, data);  //como é 1 avançar para a direita

    }else{                                              //se chegar ao fim do código 
        return(creatNode(data));                        //criar um nó com o respetivo valor
    }
    return(node);                                       //retornar a árvore
}
int checkTree(struct node* node, char *data){
    int value = NULL_INT;
    if(*data == '0'){
        *(data++);
        value = checkTree(node->left, data);

    }else if(*data == '1'){
        *(data++);
        value = checkTree(node->right, data);
    }else{
        if(node->left == NULL && node->right == NULL) return node->data;        //se os nós à esquerda e direita forem nulos (existe valor), retornar esse valor
        else return NULL_INT;                                                   //retornar nulo se não existir valor
        
    }
    return value;
}
int readOutput(short int *buffer, int length, unsigned char *codeBuffer, int cBufferLength){
    int i = 0, code;
    while( i < cBufferLength){
        code = codeBuffer[i];
        if (leftover > 0) {                             //existem bits guardados
            code = (leftoverBits << 8) + code;          
            buffer[length] = code;
            length++;
            
            leftover = 0; // no leftover now
        } else {
            i = i + 1;
            int nextCode = codeBuffer[i];
            leftoverBits = nextCode & 0xF;              // guardar os últimos 4 bits (00001111)
            leftover = 1;                               // indicar que existem bits guardados
            buffer[length] = (code << 4) + (nextCode >> 4);
            length++;
        }
        i = i + 1;
    }
    return length;
}
void lzwDecoder(int fdOriginal, unsigned char  file[], int byteCounter){
    dicionaryInit();
    char finalBuffer[M_SIZE] = {0};
    short int lzwBuffer[M_SIZE];                                            //buffer onde vão ser guardados os bytes para a descompressão LZW (12bits)
    int finalIndex = 0, lzwIndex = 0, nextCode = 256, codeword, pCodeword;
    char letter[128];
    leftover = 0;                                                           //novo bloco, não existem bits guardados
    
    lzwIndex = readOutput(lzwBuffer, lzwIndex, file, byteCounter);          // conversão de 8bits para 12bits e armazenar em lzwBuffer
    
    
    pCodeword = lzwBuffer[0];
    finalBuffer[finalIndex] = pCodeword;
    finalIndex++;
    for (int j = 1; j < lzwIndex; j++){
        codeword = lzwBuffer[j];
        if(codeword >= nextCode){                                           //se for maior significa que não está no dicionário
            int pLength = dict[pCodeword].length;

            char *pLetter = dictionaryGetLetter(pCodeword);
            memcpy(letter,pLetter,pLength);
            letter[pLength] = pLetter[0];
            dictionaryAdd(letter, pLength + 1, nextCode);                   //adicionar a nova palavra ao dicionário (palavra do código anterior + 1ªletra do código anterior)
            nextCode++;
            memcpy(&finalBuffer[finalIndex],letter, pLength + 1);           //adicionar a palavra ao buffer que vai ser escrito no ficheiro final
            finalIndex += (pLength+ 1);
        }else{                                                              //está no dicionário
            int pLength = dict[pCodeword].length;
            int cLength = dict[codeword].length;

            char *pLetter = dictionaryGetLetter(pCodeword);
            char *cLetter = dictionaryGetLetter(codeword);
            memcpy(letter,pLetter,pLength);
            letter[pLength] = cLetter[0];
            dictionaryAdd(letter, pLength + 1,nextCode);                    //adicionar a nova palavra ao dicionário (palavra do código anterior + 1ªletra do código lido)
            nextCode++;
            memcpy(&finalBuffer[finalIndex],cLetter,cLength);
            finalIndex += cLength;
        }
        memset(letter, 0, sizeof(letter));
        pCodeword = codeword;
        
        if(nextCode == 4096){                                               //se o dicionário estiver todo preenchido, reiniciar
            nextCode = 256;
            memset(dict,0,4096);
            dicionaryInit();
        }
    }
    
    write(fdOriginal, finalBuffer, finalIndex);
}
struct node* readHeader(char buffer[], int size, int *packetLength, int *headerSize, char *lastBits, char *minLength){
    struct Symbols s[256] = {0};//memset(s, 0, 256);
    int numOfSymbols = 0, totalSteps = 0;
    for(int i =0;i<256;i++)memset(s[i].code,0,256);
    
    *headerSize = (unsigned char) buffer[0] + 1;
    memcpy(&*packetLength,&buffer[1],sizeof(int));
    *lastBits = buffer[5];
    unsigned char newHeader[*headerSize*2];
    memcpy(newHeader,&buffer[6],*headerSize*2);        //copiar o array que contem os simbolos e as respetivas ocurrencias para newHeader
        
    for (int i = 0; i < *headerSize; i++){         //preencher a struct para calcular o shannon
    
        s[numOfSymbols].symbol = newHeader[i];
        s[numOfSymbols].occurs = newHeader[i+*headerSize];
        numOfSymbols++;
    
    }
    struct node* node = NULL;
    node = creatNode(NULL_INT);

    shannonFano(0,numOfSymbols-1,s,0);                      //calcular a tabale de shannon
    *minLength = strlen(s[0].code);
    //printf("\nSymbol\tProbability\tCode");
    for(int i = 0; i<numOfSymbols ; i++) {
        //printf("\n(%d) %d\t%d\t%s",i,s[i].symbol,s[i].occurs,s[i].code);
        char *p = malloc(strlen(s[i].code));
        p = s[i].code;
        node  = insert(node,p,s[i].symbol);
    }
    //printf("\n");
    return node;    
}
int processPayload(unsigned char *file, char *buffer, int i, int packetLength, int lastBits, int minLength, struct node* node){
    int k = 0, index = 0;
    char array[totalSteps + 2];
    packetLength += i;
    
    while (i < packetLength){
        for (int j = 7; 0 <= j; j--) {
            array[k] = (buffer[i] & (1 << j)) ? '1' : '0';
            array[k+1] = '\0';
            if(k > minLength - 2){
                char *a = array;
                int symbol = checkTree(node, a);
                if(symbol == NULL_INT) {
                    k++;
                }else {
                    k=0;
                    memset(array, 0, sizeof(array));
                    file[index] = symbol;
                    index++;
                }
                if( (j == lastBits && i == packetLength - 1 )) break;
            }else{
                k++;
            }
        } 
        i++;   
    }
    return index;
}
void decompressMode(int filedesc, int fdOriginal, int mode){
    char buf[M_SIZE + EXTRA];
    char* buffer = (char*) malloc((M_SIZE + EXTRA) * sizeof(char)); 
    int n, nextPacketSize = -1, nextBufferSize = 0, concatPackets = 0;
    int SIZE = M_SIZE + EXTRA;

    while (((n = read(filedesc, buf, SIZE )) || nextPacketSize != 0)){

        if(debug){
            printf("\n\tBloco número %d (%d bytes)\n",block, n);
            block++;
        }

        int packetLength = 0, headerSize = 0;
        char lastBits = 0, minLength = 0;

        if(concatPackets == 1){
            buffer = (char*) realloc(buffer, nextBufferSize * sizeof(char));
            memcpy(&buffer[nextPacketSize],buf,n);                              //copiar para buffer o conteudo de nextBuf

            if(n < M_SIZE)                                                      //se o tamanho lido nao corresponder ao tamanho maximo permitido(512000)
                nextBufferSize= nextPacketSize + n;                             //o tamanho do proximo buffer será de: tamanho do proximo pacote + o total lido (n)
        }else{
            nextBufferSize = n;
            memcpy(buffer, buf, n);
        }

        clock_t beginHeader = clock();
        struct node* node = NULL;
        node = readHeader(buffer, n, &packetLength, &headerSize, &lastBits, &minLength);    //processar o cabeçalho
        
        if(debug){
            printf("-> Tempo de processamento do cabeçalho e construção da tabela shannon: %.2f ms\n",(double)(clock() - beginHeader)*1000 / CLOCKS_PER_SEC);
        }
            
        unsigned char file[M_SIZE + 500000];
        int index = 0, i = headerSize*2 + 6, totalSize = i + packetLength;
        
        clock_t beginPayload = clock();
        index = processPayload(file, buffer, i, packetLength, lastBits, minLength, node);       //processao o corpo do bloco
        
        if(debug){
            printf("-> Tempo de processamento do payload: %.2f ms\n",(double)(clock() - beginPayload)*1000 / CLOCKS_PER_SEC);
        }
        clock_t beginFinal = clock();
        if(mode == 1){                                                       //se for descompressão shannon
            write(fdOriginal,file,index);
            
        }else{                                                               //se for descompressão LZW + Shannon
            lzwDecoder(fdOriginal, file, index);
            if(debug){
                printf("-> Tempo de processamento do LZW: %.2f ms\n",(double)(clock() - beginFinal)*1000 / CLOCKS_PER_SEC);
            }
        }

        nextPacketSize = nextBufferSize - totalSize ;                       //processar o próximo pacote
        if(nextPacketSize){
            concatPackets = 1;
            char tempBuffer[nextPacketSize];
            memcpy(tempBuffer,&buffer[totalSize],nextPacketSize);
            memset(buffer,0,nextBufferSize);
            memcpy(buffer,tempBuffer,nextPacketSize);
            if(nextPacketSize > EXTRA){
                nextBufferSize = nextPacketSize + M_SIZE;
                SIZE = M_SIZE;
            } 
            else {
                nextBufferSize = nextPacketSize+M_SIZE+EXTRA; 
                SIZE = M_SIZE + EXTRA;
            }
        }
    }
}
int readOutputLZW(unsigned char *buffer, int *index) {
    int code = buffer[*index];
    if(saveLast != -1){                         //se existir último código, esta passa a ser processado
        code = saveLast;
        saveLast = -1;
        *index = *index - 1;
    } 
    if (leftover > 0) {
        code = (leftoverBits << 8) + code;
        leftover = 0;
    } else {
        *index = *index + 1;
        int nextCode = buffer[*index];
        leftoverBits = nextCode & 0xF; // save leftover, the last 00001111
        leftover = 1;
        code = (code << 4) + (nextCode >> 4);
    }
    *index = *index + 1;
    return code;
}
void decompressLZW(int filedesc, int fdOriginal){
    unsigned char buf[M_SIZE];
    char finalBuffer[M_SIZE] = {0};
    int n, length = 0, index = 0, finalIndex = 0, last = 0;
    int codeword, pCodeword, nextCode = 256; 
    char letter[128];
    dicionaryInit();
    clock_t beginTime;
    while ((n = read(filedesc, buf, M_SIZE ))){
        if(debug){
            printf("\n\tBloco número %d (%d bytes)\n",block, n);
            block++;
        }

        if(n < M_SIZE) {
            last = 1;
        }
        index = 0;
        while(index < n){
            if(finalIndex == 0){                                //se o codigo a introduzir for o primeiro
                beginTime = clock();
                pCodeword = readOutputLZW(buf, &index);
                finalBuffer[finalIndex] = pCodeword;
                finalIndex++;
            }
            if(!leftover && index == M_SIZE -1){                //se houver bits guardados e o código lido for o último, guardar esse valor e terminar de processar
                saveLast = buf[index];
                break;
            }

            codeword = readOutputLZW(buf, &index);

            if(codeword >= nextCode){
                int pLength = dict[pCodeword].length;

                char *pLetter = dictionaryGetLetter(pCodeword);
                memcpy(letter,pLetter,pLength);
                letter[pLength] = pLetter[0];
                dictionaryAdd(letter, pLength + 1, nextCode);
                nextCode++;
                memcpy(&finalBuffer[finalIndex],letter, pLength + 1);
                finalIndex += (pLength+ 1);
            }else{
                int pLength = dict[pCodeword].length;
                int cLength = dict[codeword].length;

                char *pLetter = dictionaryGetLetter(pCodeword);
                char *cLetter = dictionaryGetLetter(codeword);
                memcpy(letter,pLetter,pLength);
                letter[pLength] = cLetter[0];
                dictionaryAdd(letter, pLength + 1,nextCode);
                nextCode++;
                memcpy(&finalBuffer[finalIndex],cLetter,cLength);
                finalIndex += cLength;
            }
            memset(letter, 0, sizeof(letter));
            pCodeword = codeword;
            
            if(nextCode == 4096){
                nextCode = 256;
                memset(dict,0,4096);
                dicionaryInit();
            }

            if(finalIndex == M_SIZE){                                       //se o buffer tiver cheio, processou-se um bloco  
                write(fdOriginal, finalBuffer, finalIndex);                 //escrever o buffer no ficheiro
                if(debug){
                    printf("-> Tempo de processamento do LZW e escrita no ficheito: %.2f ms\n",(double)(clock() - beginTime)*1000 / CLOCKS_PER_SEC);
                }
                memset(finalBuffer, 0, M_SIZE);
                leftover = 0;                                               //reiniciar as variáveis necessárias
                finalIndex = 0;
                nextCode = 256;
                memset(dict,0,4096);
                dicionaryInit();
                
            }
        }
        if(last){
            write(fdOriginal, finalBuffer, finalIndex);
            if(debug) printf("-> Tempo de processamento do LZW e escrita no ficheito: %.2f ms\n",(double)(clock() - beginTime)*1000 / CLOCKS_PER_SEC);
        }
    }
}
//COMPRESSION
int dictionarySearch(int dict[][256], int parent, int child){
    if(parent == -1) return child;
    return dict[parent][child];
}
int addOutput(unsigned char *buffer, int length,  short code, struct Symbols *s){
    if (leftover > 0) {
        int previousCode = (leftoverBits << 4) + (code >> 8);
        buffer[length] = previousCode;
        length++;
        
        s[previousCode].symbol = previousCode;
        s[previousCode].occurs++;

        buffer[length] = code;
        length++;

        s[(code & 0xFF)].symbol = (code & 0xFF);
        s[(code & 0xFF)].occurs++;

        leftover = 0;
    } else {
        leftoverBits = code & 0xF; 
        leftover = 1;

        buffer[length] = (code >> 4);
        length++;

        s[(code >> 4)].symbol = code >> 4;
        s[(code >> 4)].occurs++;
    }
    return length;
}
int addOutputLzw(short code, unsigned char *buffer, int length){
     if (leftover > 0) {                                                               //existem bits guardados
        int previousCode = (leftoverBits << 4) + (code >> 8);
        buffer[length] = previousCode;
        length++;
        buffer[length] = code;
        length++;
        leftover = 0;
    } else {
        leftoverBits = code & 0xF;                                                     // guardar os últimos 4 bits (00001111)
        leftover = 1;                                                                  // indicar que existem bits guardados
        buffer[length] = (code >> 4);
        length++;
    }
    return length;
}
int checkSymbol(unsigned char val, struct Symbols s[], int total){
    int i;
    for(i = 0; i < total; i++){
        if(s[i].symbol == val){
            return i;
        }
    }
    return -1;
}
int compare(const void *s1, const void *s2){
    struct Symbols *e1 = (struct Symbols *)s1;
    struct Symbols *e2 = (struct Symbols *)s2;
    return e1->occurs < e2->occurs;
}
int sortDesc(struct Symbols s[]){
    int i, j, total = 0;

    qsort(s, 256, sizeof(struct Symbols), compare);
    for (i = 0; i < 256; i++){
        if(s[i].occurs > 0) total++;
        else break;
        
    }
    return total;
}
void compressFile(int fd,struct Symbols s[], unsigned char buffer[],int n, int numSymbols, int lastPacket){
    char newBuffer[M_SIZE+500000] = {0};
    int bitCounter = 0, byteCounter = 0, index = 0;;
    int symbolMap[256];
    memset(symbolMap,0,256* sizeof(int));
    
    clock_t begin = clock();

    for(int i = 0;i < numSymbols;i++){
        symbolMap[s[i].symbol] = i;
    }
    //preencher o body
    for(int i = 0;i < n;i++){
        /*for(int j = 0;j<numSymbols;j++){
            if(buffer[i] == s[j].symbol) {
                index = j;
                break;
            }
        }*/
        index = symbolMap[buffer[i]];
        for(int j = 0; j<strlen(s[index].code); j++){
            if(s[index].code[j] == '1') newBuffer[byteCounter] |= 1 << (7-bitCounter);
            bitCounter++;
            if(bitCounter==8){
                bitCounter = 0;
                byteCounter++;
            }
        }     
    }
    
    //caso o ultimo byte nao esteja todo preenchido
    char lastBits = 0;
    if(bitCounter > 0){
        byteCounter++;
        lastBits = 8-bitCounter;
    }
    //preencher o header
    unsigned char header_size = numSymbols - 1;
    unsigned char header[numSymbols*2];
    for (int i = 0; i < numSymbols; i++){
        header[i] = s[i].symbol;
        header[i+numSymbols] = s[i].occurs;
    }
    if(debug){
        printf("-> Tempo de construção do bloco comprimido: %.2f ms\n",(double)(clock() - begin)*1000 / CLOCKS_PER_SEC);
        printf("-> Tamanho do bloco comprimido: %d bytes\n",byteCounter);
        compressedSize += (sizeof(header_size) + sizeof(byteCounter) + sizeof(lastBits) + sizeof(header) + byteCounter);
    }
    
    write(fd, &header_size,sizeof(header_size));
    write(fd, &byteCounter,sizeof(byteCounter));
    write(fd, &lastBits,sizeof(lastBits));
    write(fd, header,sizeof(header));
    write(fd, newBuffer,byteCounter);
} 
void shannonEncoder(int fdCompressed, char buffer[], int size){
    struct Symbols s[256] = {0};
    int allSymbols = 0, numOfSymbols = 0, totalSteps = 0;
    int symbolMap[256];

    clock_t beginSymbols = clock();
    for(int i =0;i<256;i++){
        memset(s[i].code,0,256);
        symbolMap[i] = -1;
    }
    
    for(int i = 0; i < size; i++){
        //int pos = checkSymbol((unsigned char)buffer[i], s, numOfSymbols);
        int pos = symbolMap[(unsigned char)buffer[i]];
        if(pos == -1){
            s[numOfSymbols].symbol = (unsigned char)buffer[i];
            s[numOfSymbols].occurs = 1;
            s[numOfSymbols].prob = 0.0;
            symbolMap[(unsigned char)buffer[i]] = numOfSymbols;
            numOfSymbols++;
        }else{
            s[pos].occurs++;
        }
        allSymbols++;
    }
    clock_t beginShannon = clock();
    sortDesc(s);
    if(debug){
        printf("-> Tempo de construção da tabela de símbolos: %.2f ms\n",(double)(clock() - beginSymbols)*1000 / CLOCKS_PER_SEC);
        double entropy = 0; 
        for (int i = 0; i < numOfSymbols; i++){
            s[i].prob = (float) s[i].occurs/allSymbols;
            entropy += (double) s[i].prob * log2(1/s[i].prob);
        }
        printf("-> Entropia: %.2f bits/símbolo\n",entropy);
    }

    for (int i = 0; i < numOfSymbols; i++){
        if(s[i].occurs > 255){
            while(s[i].occurs > 255) {
                for(int j = 0; j<numOfSymbols;j++){
                    if(s[j].occurs != 1)
                        s[j].occurs >>= 1;
                } 
            }
        }
    }
    shannonFano(0,numOfSymbols-1,s,0); 

    if(debug){
        printf("-> Tempo de tratamento e cálculo do Shannon: %.2f ms\n",(double)(clock() - beginShannon)*1000 / CLOCKS_PER_SEC);
        double avgLength = 0; 
        //printf("\nSímbolo\tProbabilidade\t\tCódigo");
        for (int i = 0; i < numOfSymbols; i++){
            //printf("%d\t%.2f\t\t%s\n",s[i].symbol,s[i].prob,s[i].code);
            avgLength += (double) s[i].prob * strlen(s[i].code);
        }
        printf("-> Comprimento médio: %.2f dig. binários/símbolo\n",avgLength);
    }
    compressFile(fdCompressed, s, buffer, size, numOfSymbols, 0);

}
void lzwShannonEncoder (int fdCompressed, char buffer[], int size){
    struct Symbols s[256] = {0};
    int dict[4096][256];
    int allSymbols = 0, numOfSymbols = 0, totalSteps = 0; 
    int nextCode = 256, codeValue = 0, tempIndex = 0;
    unsigned char nextChar;
    short int currentChar = -1;
    unsigned char tempBuffer[M_SIZE+500000] = {0};

    for(int i =0;i<256;i++)memset(s[i].code,0,256);
    memset(dict,-1,4096*256*sizeof dict[0][0]);

    clock_t beginLZW = clock();

    for(int i = 0; i < size; i++){

        nextChar = (unsigned char)buffer[i];
        
        if((codeValue = dictionarySearch(dict, currentChar,nextChar) ) != -1){
            currentChar = codeValue;
        }else{
            tempIndex = addOutput(tempBuffer, tempIndex, currentChar,s);    //converter os 12 bits para 1 byte (guardando os últimos 4 bits)
            dict[currentChar][nextChar] = nextCode;                         //Adicionar à tabela do dicionário
            currentChar = nextChar;
            nextCode++;
        }

        if(nextCode == dictSize){                                           //reiniciar o dicionário
            nextCode = 256;
            memset(dict,-1,4096*256*sizeof dict[0][0]);
        }
        allSymbols++;
    }
    //printf("DONE LZW!\n");
    tempIndex = addOutput(tempBuffer, tempIndex, currentChar,s);
    
    if(leftover > 0){
        tempBuffer[tempIndex] = (leftoverBits << 4);
        tempIndex++;
        s[(leftoverBits << 4)].symbol = (leftoverBits << 4);
        s[(leftoverBits << 4)].occurs++;
        leftover = 0;
    }
    clock_t beginShannon = clock();
    numOfSymbols = sortDesc(s);

    if(debug){
        printf("-> Tempo de processamento do LZW: %.2f ms\n",(double)(beginShannon - beginLZW)*1000 / CLOCKS_PER_SEC);
        double entropy = 0; 
        for (int i = 0; i < numOfSymbols; i++){
            s[i].prob = (float) s[i].occurs/allSymbols;
            entropy += (double) s[i].prob * log2(1/s[i].prob);
        }
        printf("-> Entropia: %.2f bits/símbolo\n",entropy);
    }

    for (int i = 0; i < numOfSymbols; i++){
        if(s[i].occurs > 255){
            while(s[i].occurs > 255) {
                for(int j = 0; j<numOfSymbols;j++){
                    if(s[j].occurs != 1)
                        s[j].occurs >>= 1;
                } 
            }
        }
    }
    shannonFano(0,numOfSymbols-1,s,0); 

    if(debug){
        printf("-> Tempo de tratamento e cálculo do Shannon: %.2f ms\n",(double)(clock() - beginShannon)*1000 / CLOCKS_PER_SEC);
        double avgLength = 0; 
        //printf("\nSímbolo\tProbabilidade\t\tCódigo");
        for (int i = 0; i < numOfSymbols; i++){
            //printf("%d\t%.2f\t\t%s\n",s[i].symbol,s[i].prob,s[i].code);
            avgLength += (double) s[i].prob * strlen(s[i].code);
        }
        printf("-> Comprimento médio: %.2f\n",avgLength);
    }

    compressFile(fdCompressed, s, tempBuffer, tempIndex, numOfSymbols, 0);
}
void lzwEncoder(int fdCompressed, char buffer[], int size){
    int dict[4096][256];
    int nextCode = 256, codeValue = 0, tempIndex = 0;
    unsigned char tempBuffer[M_SIZE + 500000] = {0};
    unsigned char nextChar;
    short int currentChar = -1;

    memset(dict,-1,4096*256*sizeof dict[0][0]);

    lzwTime = clock();

    for(int i = 0; i < size; i++){
        
        nextChar = (unsigned char)buffer[i];
        
        if((codeValue = dictionarySearch(dict, currentChar,nextChar) ) != -1){
            currentChar = codeValue;
        }else{
            tempIndex = addOutputLzw(currentChar, tempBuffer, tempIndex);
            dict[currentChar][nextChar] = nextCode; 
            currentChar = nextChar;
            nextCode++;
        }

        if(nextCode == dictSize){
            nextCode = 256;
            memset(dict,-1,4096*256*sizeof dict[0][0]);
        }
    }
    tempIndex = addOutputLzw(currentChar, tempBuffer, tempIndex);
    if(leftover > 0){
        tempBuffer[tempIndex] = (leftoverBits << 4);
        tempIndex++;
        leftover = 0;
    }
    if(debug){
        printf("->Tempo de processamento do LZW: %.2f ms\n",(double)(clock() - lzwTime)*1000 / CLOCKS_PER_SEC);
        printf("->Tamanho do bloco comprimido: %d bytes\n",tempIndex);
        compressedSize += tempIndex;
    }
    
    write(fdCompressed, tempBuffer, tempIndex);

}

int main(int argc, char *argv[]){
    int fileIndex = 3;
    if (argc > 2) {
        clock_t begin = clock();

        if(strcmp(argv[1], "-c") == 0){             //COMPRESSÃO
            char buffer[M_SIZE];                    //buffer para conter os bytes do inputFile
            int inputFile, n = 0;

            if(argc == 5) {                             //Aplicar mode de verbose
                debug = 1;
                fileIndex = 4;
            }
            int outputFile = open("compressed.txt", O_RDWR | O_CREAT | O_TRUNC | O_APPEND, 0666);
            inputFile = open(argv[fileIndex], O_RDONLY);
            if (strcmp(argv[2], "-ls") == 0) {          // compressão LZW+Shannon
                printf("COMPRESSÂO LZW + SHANNON INICIADA...\n");

                while (n = read(inputFile, buffer, M_SIZE)){
                    if(debug){
                        printf("\n\tBloco número %d (%d bytes)\n",block, n);
                        fileSize += n;
                        block++;
                    }
                    lzwShannonEncoder(outputFile, buffer,n);
                }
                if(debug) printf("\nNível de compressão: %.2f",(float)fileSize/compressedSize);
                
            } else if(strcmp(argv[2], "-s") == 0){      // compressão Shannon
                printf("COMPRESSÂO SHANNON INICIADA...\n");

                while (n = read(inputFile, buffer, M_SIZE)){
                    if(debug){
                        printf("\n\tBloco número %d (%d bytes)\n",block, n);
                        fileSize += n;
                        block++;
                    }
                    shannonEncoder(outputFile,buffer, n);
                }
                if(debug) printf("\nNível de compressão: %.2f",(float)fileSize/compressedSize);
            } else if(strcmp(argv[2], "-l") == 0){      //compressão LZW
                printf("COMPRESSÂO LZW INICIADA...\n");

                while (n = read(inputFile, buffer, M_SIZE)){
                    if(debug){
                        printf("\n\tBloco número %d (%d bytes)\n",block, n);
                        fileSize += n;
                        block++;
                    }
                    lzwEncoder(outputFile,buffer, n); 
                }
                if(debug) printf("\nNível de compressão: %.2f",(float)fileSize/compressedSize);
            }else printf("Invalid Type!\n");
            
            close(outputFile);
            close(inputFile);
        }
        else if (strcmp(argv[1], "-d") == 0){           //DESCOMPRESSÃO
            if(argc == 5) {                             //Aplicar mode de verbose
                debug = 1;
                fileIndex = 4;
            }
            char newFile[4 + strlen(argv[fileIndex])];
            strcpy(newFile,"NEW_");
            int outputFile = open(strcat(newFile,argv[fileIndex]), O_RDWR | O_CREAT | O_TRUNC | O_APPEND, 0666);
            int inputFile = open("compressed.txt", O_RDONLY);

            if (strcmp(argv[2], "-ls") == 0) {                      // decompression LZW+Shannon
                printf("DESCOMPRESSÂO LZW + SHANNON INICIADA...\n");
                decompressMode(inputFile, outputFile, 2);
            } else if(strcmp(argv[2], "-s") == 0){                  // decompression Shannon
                printf("DESCOMPRESSÂO SHANNON INICIADA...\n");
                decompressMode(inputFile, outputFile, 1);
            } else if(strcmp(argv[2], "-l") == 0){                  // decompression LZW
                printf("DESCOMPRESSÂO LZW INICIADA...\n");
                decompressLZW(inputFile,outputFile);
            }else printf("Invalid Type!\n");

            close(outputFile);
            close(inputFile);
        }
        clock_t end = clock();
        double time_spent = (double)(end - begin) / CLOCKS_PER_SEC;
        printf("\nTempo de execução total: %.2f ms\n",time_spent*1000);

    } else {
        printf("Usage: TP2 [MODE] [TYPE] [OPTION] [FILENAME]\n\n");
        printf("MODE required\n  -c,\tCompression mode\n  -d,\tDecompression mode\n");
        printf("\nTYPE required\n  -ls,\tApply LZW and Shannon compression\n  -s,\tApply only Shannon compression\n  -l,\tApply only LZW compression\n");
        printf("\nOPTION\n  -v,\tApply debug mode\n\n");
    }
    

    
    return 0;
}