#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <math.h>
#include <string.h>
#define arrayLength(array) (sizeof((array))/sizeof((array)[0]))

struct Symbols {
    char symbol;
    int occurs;
    float prob;
    char code[256];
};
int totalSteps = 0;
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
int checkSymbol(char val[], struct Symbols s[],int total){
    int i, flag = 0;
    for(i = 0; i < total; i++){
        int j = 0;
        if(strcmp(val,s[i].code) == 0){
            flag=1;
            //printf("FOUNDED: %c\n",i);
        } 
        //else printf("not equals\n");

        if(flag) break;
        
    }
    if(flag) return i;
    return -1;
}

int main(int argc, char *argv[]){
    char c, fileName[16];
    struct Symbols s[256];
    for(int i =0;i<256;i++)memset(s[i].code,0,256);
    int allSymbols = 0, numOfSymbols = 0;
    int fd = open("original.txt", O_RDWR | O_CREAT | O_TRUNC | O_APPEND, 0666);

    clock_t begin = clock();

    printf("Decompressing...\n");
    char buf[512000];
    char* nextBuf;
    int nextPacketSize = -1;
    int nextBufferSize = 0;
    int n;
    int last = 0;
    int filedesc = open("compressed.txt", O_RDONLY);
    int concatPackets = 0;
    while ((n = read(filedesc, buf, 512000)) || nextPacketSize != 0){
        memset(s, 0, 256);
        numOfSymbols = 0;
        allSymbols = 0;
        totalSteps = 0;
        char* buffer;
        if(concatPackets == 1){
            //printf("nextPacket\n");
            char temp[512000];
            buffer = (char*) malloc( nextBufferSize * sizeof(char));        //alocar nextBufferSize de memoria para o array (nextBufferSize = tamanho do proximo pacote + total que se pode ler(n) )
            memcpy(temp,buf,n);
            memcpy(&nextBuf[nextPacketSize],buf, n);                        //preencher nextBuffer com o conteudo recebido (conteudo do proximo pacote + conteudo lido nesta iteraçao)
            memcpy(buffer,nextBuf,nextBufferSize);                          //copiar para buffer o conteudo de nextBuf
            if(n < 512000){                                                 //se o tamanho lido nao corresponder ao tamanho maximo permitido(512000)
                last = 1;                                                       //indicar que é o ultimo pacote
                nextBufferSize= nextPacketSize + n;                             //o tamanho do proximo buffer será de: tamanho do proximo pacote + o total lido (n)
            } 

        }else{
            buffer = (char*) malloc(512000 * sizeof(char));                 //caso seja o primeiro buffer lido
            nextBufferSize = n;
            
            memcpy(buffer, buf, n);
            
        }
        unsigned int headerSize = 0;
        memcpy(&headerSize, buffer, sizeof(int));
        //printf("header size: %d\n",headerSize);
        unsigned int packetSize = 0;
        memcpy(&packetSize, &buffer[4], sizeof(int));
        //printf("packet size: %d\n",packetSize);
        unsigned char newHeader[headerSize];
        unsigned char lastBits = buffer[8];
        //printf("last bits: %d\n",lastBits);
        memcpy(newHeader,&buffer[9],headerSize);        //copiar o array que contem os simbolos e as respetivas ocurrencias para newHeader
        for (int i = 0; i < headerSize/2; i++){         //preencher a struct para calcular o shannon
            
            s[numOfSymbols].symbol = newHeader[i];
            s[numOfSymbols].occurs = newHeader[i+headerSize/2];
            s[numOfSymbols].prob = 0.0;
            numOfSymbols++;
        
        }
        nextPacketSize = nextBufferSize-(packetSize + headerSize + 9);          //tamanho do proximo pacote
        //printf("nextPacketSize: %d\n",nextBufferSize-(packetSize + headerSize + 9));
        //printf("currentPacketSize: %d\n",packetSize + headerSize + 9);
        if(nextPacketSize != 0 ){                                   //se houver proximo pacote, copiar o conteudo para um array temporario 
            concatPackets = 1;
            nextBuf = (char*) malloc((nextPacketSize+512000)* sizeof(char));
            nextBufferSize = nextPacketSize+512000;
            //printf("next size: %d\n",nextBufferSize);           //tamanho do proximo buffer
            memcpy(nextBuf,&buffer[packetSize + headerSize + 9],nextBufferSize);
        }
        char body[packetSize*8];
        int bitCounter = 0;
        for (int i = headerSize + 9; i < packetSize + headerSize + 9 ; i++){
            for (int j = 7; 0 <= j; j--) {
                body[bitCounter] = (buffer[i] & (1 << j)) ? '1' : '0';
                bitCounter++;
                //printf("%c", (buffer[i] & (1 << j)) ? '1' : '0');
            }
        }
        printf("\n");
        body[bitCounter] = '\0';

        shannonFano(0,numOfSymbols-1,s,0);

        printf("\nSymbol\tProbability\tCode");
        for(int i=0; i<numOfSymbols; ++i) {
            printf("\n%c\t%d\t\t",s[i].symbol,s[i].occurs);
            for(int j=0; j<=totalSteps; j++) { 
                printf("%c",s[i].code[j]);
            }
        }
        printf("\n\n");
        int map[255];
        for(int i = 0;i < numOfSymbols;i++){
            map[s[i].symbol] = i;
        }
        int j = 0;
        int next = 0;
        char array[totalSteps+2];
        char file[bitCounter];
        int byteCounter = 0;

        for(int i = 0;i < bitCounter;i++){                  //percorrer o corpo do ficheiro comprimido 
            //printf("body: %c\n",body[i]);
            array[j] = body[i];
            array[j+1] = '\0';
            int symbol = checkSymbol(array,s,numOfSymbols);     //verificar qual é o simbolo 
            if(symbol == -1) {
                j++;
                next=1;
            }
            else {
                j=0;
                //printf("symbol: %c\n",s[symbol].symbol);
                file[byteCounter] = s[symbol].symbol;
                byteCounter++;
                memset(array, 0, sizeof(array));
                if(i == (bitCounter-lastBits-1))break;          //se chegar ao fim, para o ciclo
            }
            
        }
        file[byteCounter] = '\0';
        printf("Pacote descomprimido\nA escrever no ficheiro...\n");
        write(fd, file,byteCounter);                            //escrever os simbolos descodificados no ficheiro
        
    }
    close(fd);

    return 0;
}