#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <math.h>

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

int checkSymbol(char val, struct Symbols s[], int total){
    int i;
    for(i = 0; i < total; i++){
        if(s[i].symbol == val){
            return i;
        }
    }
    return -1;
}

void sortDesc(struct Symbols s[], int total){
    int i, j;
    struct Symbols temp;
    
    for (i = 0; i < total - 1; i++){
        for (j = 0; j < (total - 1-i); j++){
            if (s[j].occurs < s[j + 1].occurs){
                temp = s[j];
                s[j] = s[j + 1];
                s[j + 1] = temp;
            } 
        }
    }
}

void printSymbolBits(char val) {
  for (int i = 7; 0 <= i; i--) {
    printf("%c", (val & (1 << i)) ? '1' : '0');
  }
}
void compressFile(int fd,struct Symbols s[], char buffer[],int n, int numSymbols){
    int symbolMap[255];
    char newBuffer[512000] = {0};
    int bitCounter = 0, byteCounter = 0;
    unsigned int nextPacket = 0;
    
    for(int i = 0;i < numSymbols;i++){
        symbolMap[s[i].symbol] = i;
    }
    //preencher o body
    for(int i = 0;i < n;i++){
        int index = symbolMap[buffer[i]];
        int j = 0;
        for(int j = 0; j<strlen(s[index].code); j++){
            //printf("%c",s[index].code[j]);
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
    nextPacket = byteCounter;
    int headerSize = numSymbols*2;
    unsigned char header[numSymbols*2];
    for (int i = 0; i < numSymbols; i++){
        header[i] = s[i].symbol;
        /*if(s[i].occurs > 256){
            while(s[i].occurs > 256) {
                for(int j = 0; j<numSymbols;j++){
                        s[j].occurs >>= 1;
                } 
            }
        }*/
        header[i+numSymbols] = s[i].occurs;
    }
    
   /* for(int i = 0;i<20;i++){
        for (int j = 7; 0 <= j; j--) {
            printf("%c", (newBuffer[i] & (1 << j)) ? '1' : '0');
        }
    }
    printf("\n");
    printf("packetSize: %d\n",nextPacket);*/
    printf("Pacote comprimido.\nA escrever no ficheiro...\n");
    write(fd, &headerSize,sizeof(headerSize));
    write(fd, &nextPacket,sizeof(nextPacket));
    write(fd, &lastBits,sizeof(lastBits));
    write(fd, header,sizeof(header));
    write(fd, newBuffer,byteCounter);
    
    
    //printf("%d bytes compressed.\n",n);

}   
int main(int argc, char *argv[]){
    int fdc = open("compressed.txt", O_RDWR | O_CREAT | O_APPEND, 0666);
    char c, fileName[16];
    struct Symbols s[256];
    for(int i =0;i<256;i++)memset(s[i].code,0,256);
    int fd, n = 0, allSymbols = 0, numOfSymbols = 0;
    char buffer[512000];

    if(argc == 2){
        fd = open(argv[1], O_RDONLY);
    }else if( argc > 2 ) {
        printf("Too many arguments supplied.\n");
        return 0;
    }else {
        printf("Enter the file name: \n");
        read(0,fileName,16);
        //printf("\n%s",fileName);
        fd = open(strtok(fileName, "\n"), O_RDONLY);
    }
    
    clock_t begin = clock();

    while (n = read(fd, buffer, 512000)){
        memset(s, 0, 256);
        numOfSymbols = 0;
        allSymbols = 0;
        totalSteps = 0;

        for(int i = 0;i < n;i++){
            int pos = checkSymbol(buffer[i], s, numOfSymbols);
            if(pos == -1){
                s[numOfSymbols].symbol = buffer[i];
                s[numOfSymbols].occurs = 1;
                s[numOfSymbols].prob = 0.0;
                numOfSymbols++;
            }else{
                s[pos].occurs++;
            }
            allSymbols++;
        }
        printf("num of symbols: %d\n",numOfSymbols);
        //ordenar os simbolos
        sortDesc(s,numOfSymbols);
        for (int i = 0; i < numOfSymbols; i++){
            if(s[i].occurs > 256){
                while(s[i].occurs > 256) {
                    for(int j = 0; j<numOfSymbols;j++){
                            s[j].occurs >>= 1;
                    } 
                }
            }
        }
        //Calculo da probablidade de cada simbolo e entropia
        for(int i = 0;i<numOfSymbols;i++){
            s[i].prob =(float) s[i].occurs/allSymbols;
        }

        shannonFano(0,numOfSymbols-1,s,0); //inicio, fim, simbolos, saltos

        printf("\nSymbol\tProbability\tCode");
        for(int i=0; i<numOfSymbols; ++i) {
            printf("\n%c\t%d\t\t",s[i].symbol,s[i].occurs);
            for(int j=0; j<=totalSteps; j++) { 
                printf("%c",s[i].code[j]);
            }
        }
        printf("\n\n");

        compressFile(fdc, s, buffer, n, numOfSymbols);
        
        
    }
    clock_t end = clock();
        double time_spent = (double)(end - begin) / CLOCKS_PER_SEC;
        
        printf("Execution time: %.2f ms\n",time_spent*1000);
    close(fdc);
    close(fd);
    
    return 0;
}