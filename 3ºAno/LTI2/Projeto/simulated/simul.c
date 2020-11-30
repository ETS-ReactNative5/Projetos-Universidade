#include <arpa/inet.h>
#include <limits.h>
#include <math.h>
#include <netinet/in.h>
#include <net/if.h> 
#include <poll.h>
#include <signal.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <time.h>
#include <unistd.h>

#define MAX 2048
#define MAX_S 32
#define MAX_NS 30

#define PM 2000
#define PA 4000
#define NS 10
#define ISS 2
#define T_COMS 1
#define T_ACCEPT 2
#define T_START 3
#define T_DATA 4
#define T_STOP 5
#define L_DATA "simuldb.txt"

#define PORT 12900
#define IP_SERVER "A.B.C.D"

typedef char byte;
struct sockaddr_in server;
bool *started = false;
unsigned char ip0,ip1,ip2,ip3;
struct message {
  byte tp;        //0=setup, -1=seterr, 1=start, 2=data, -2=error, 3=stop, 4=show
  unsigned long ts;    //setup, seterr, start, data, error, stop, exit
  byte iss;        //data
  unsigned int pm;         //start, data
  unsigned int pa;         //start, data
  unsigned char ns;         //start, data
  byte er;        //error
  unsigned int ad;    //erro
  long int tsi;   //show
  long int tsf;   //show
  bool se;
  char sr[MAX_S];    //stop
  char com[MAX_S];   //setup
  short sax[MAX_NS]; //data
  short say[MAX_NS]; //data
  short saz[MAX_NS]; //data
  short sgx[MAX_NS]; //data
  short sgy[MAX_NS]; //data
  short sgz[MAX_NS]; //data
  short st[MAX_NS];
};
/*void getIPAddress(){
  int fd;
  struct ifreq ifr;
  fd = socket(AF_INET, SOCK_DGRAM, 0);

  // I want to get an IPv4 IP address
  ifr.ifr_addr.sa_family = AF_INET;

  // I want IP address attached to "eth0"
  strncpy(ifr.ifr_name, "wlo1", IFNAMSIZ-1);

  ioctl(fd, SIOCGIFADDR, &ifr);

  close(fd);

  // display result
  printf("%s\n", inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr));
  sscanf(inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr),"%hhd.%hhd.%hhd.%hhd",&ip0,&ip1,&ip2,&ip3);
  printf("%d %d %d %d\n", ip0,ip1,ip2,ip3);
}*/

void handshake(int s, struct message message){
  struct timeval tv;
  tv.tv_sec = 0;
  tv.tv_usec = 500000;
  char messageString[MAX];
  int n,len;
  bool acceptHS = false;
  

  //getIPAddress(); 
  message.tp = T_COMS;
  //message.ad = "myip";
  message.iss = ISS;
  messageString[0] = T_COMS;
  messageString[1] = ISS;
  printf("%d %d\n", messageString[0], messageString[1]);

  if (setsockopt(s, SOL_SOCKET, SO_RCVTIMEO,&tv,sizeof(tv)) < 0) {
    perror("Error");
  }
  printf("A estabelecer conexão com o concentrador...\n");
  while(!acceptHS){
    //write(1,"while",5);
    n = sendto(s, messageString, 2, 0,(struct sockaddr *)&server, sizeof(server)); 

    n = recvfrom(s, messageString, MAX, 0,(struct sockaddr *)&server, &len);

    
    if(messageString[0] == T_ACCEPT){
      acceptHS = true;
      printf("Conexão estabelecida com sucesso!\n");
    }

  }
}

struct message processData(char *messageString){

  struct message message;

  if (messageString[0] == T_START){

      message.tp = T_START;

      message.ts = (unsigned char) messageString[1] * pow(2, 24) + (unsigned char) messageString[2] * pow(2, 16) + (unsigned char) messageString[3] * pow(2, 8) + (unsigned char) messageString[4];

      message.pm = (unsigned char) messageString[5] * pow(2, 24) + (unsigned char) messageString[6] * pow(2, 16) + (unsigned char) messageString[7] * pow(2, 8) + (unsigned char) messageString[8];
      if(message.pm == 0) message.pm = PM;
      //printf("pm %d %x\n", message.pm, message.pm);
      message.pa = (unsigned char) messageString[9] * pow(2, 24) + (unsigned char) messageString[10] * pow(2, 16) + (unsigned char) messageString[11] * pow(2, 8) + (unsigned char) messageString[12];
      if(message.pa == 0) message.pa = PA;
      //printf("pa %d %x\n", message.pa, message.pa);
      message.ns = (unsigned char) messageString[13];
      if(message.ns == 0) message.ns = NS;

      printf("if start %d | %d %d %d\n", message.tp, message.pm, message.pa, message.ns);

      *started = true;
  }
  if (messageString[0] == T_STOP){
    message.tp = T_STOP;
      message.ts = (unsigned char) messageString[1] * pow(2, 24) + (unsigned char) messageString[2] * pow(2, 16) + (unsigned char) messageString[3] * pow(2, 8) + (unsigned char) messageString[4];
    message.pm = 0;
    message.pa = 0;
    message.ns = 0;
      *started = false;
  }
  printf("got %d %ld %d %d %d\n", messageString[0], message.ts,message.pm,message.pa,message.ns);

  return message;
}

struct message readData (FILE *fp,int s, struct message message) {
  int n = 0;
  char messageString[MAX];
  
  
  if ( fp ) {
    message.tp = T_DATA;
    while (fgets(messageString, MAX, fp) && n < message.ns) {
      printf("> %s\n", messageString);
      sscanf(messageString, "%hd %hd %hd %hd %hd %hd %hd",&message.sax[n],&message.say[n],&message.saz[n],&message.sgx[n],&message.sgy[n],&message.sgz[n],&message.st[n] );
      //memset(messageString, 0, MAX);
      printf("%d %d %d %d %d %d %d\n",message.sax[n],message.say[n],message.saz[n],message.sgx[n],message.sgy[n],message.sgz[n],message.st[n] );
      //tempMessage = stringToStruct(messageString);
      n++;
      usleep(message.pa);
      write(1,"time\n",5);
    }
  }
  printf("reading data %d | %d %d %d\n", message.tp, message.pm, message.pa, message.ns);
  //fclose(fp);
  return message;
}

unsigned char *structToString(struct message message){
  unsigned char messageString[MAX];
  unsigned char *messageStringT;
  memset(messageString, 0, MAX);
  message.tp = T_DATA;
  message.iss = ISS;
  printf("tp %d\n", message.tp);
  messageString[0] = message.tp;

  printf("ts %ld\n", message.ts);
  messageString[1] = message.ts >> 24;
  messageString[2] = message.ts >> 16;
  messageString[3] = message.ts >> 8;
  messageString[4] = message.ts;

  printf("iss %d\n", message.iss);
  messageString[5] = message.iss;

  printf("pm %d\n", message.pm);
  messageString[6] = message.pm >> 24;
  messageString[7] = message.pm >> 16;
  messageString[8] = message.pm >> 8;
  messageString[9] = message.pm;

  printf("pa %d\n", message.pa);
  messageString[10] = message.pa >> 24;
  messageString[11] = message.pa >> 16;
  messageString[12] = message.pa >> 8;
  messageString[13] = message.pa;

  printf("ns %d\n", message.ns);
  messageString[14] = message.ns;

  for (int n = 0; n < message.ns; n ++) {
    //token = strtok(NULL, "|");
    messageString[15 + 14 * n] = message.sax[n]>>8;
    messageString[16 + 14 * n] = message.sax[n];
    messageString[17 + 14 * n] = message.say[n]>>8;
    messageString[18 + 14 * n] = message.say[n];
    messageString[19 + 14 * n] = message.saz[n] >> 8;
    messageString[20 + 14 * n] = message.saz[n];
    messageString[21 + 14 * n] = message.sgx[n] >> 8;
    messageString[22 + 14 * n] = message.sgx[n];
    messageString[23 + 14 * n] = message.sgy[n]>> 8;
    messageString[24 + 14 * n] = message.sgy[n];
    messageString[25 + 14 * n] = message.sgz[n] >> 8;
    messageString[26 + 14 * n] = message.sgz[n];
    messageString[27 + 14 * n] = message.st[n] >> 8;
    messageString[28 + 14 * n] = message.st[n];

  } 
  messageStringT = messageString;
  return messageStringT;
}
int main () {
     //timeout to recvfrom
  struct message message;
  struct message *tempMessage;
  char *messageString;
  messageString = malloc(MAX);
  int s,n,len;
  unsigned int mSeconds; 
  FILE *fp;
  unsigned int *pmT;
  unsigned int *paT;
  unsigned char *nsT;


  if ((s = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
  {
     perror("socket()");
     exit(1);
  }

  /* Set up the server name */
  server.sin_family      = AF_INET;            /* Internet Domain    */
  server.sin_port        = htons(PORT);               /* Server Port        */
  server.sin_addr.s_addr = inet_addr(IP_SERVER); /* Server's Address   */

  

  //write(1,"handshake: ",11);

  handshake(s,message);

  struct timeval tv;
  tv.tv_sec = 0;
  tv.tv_usec = 0;
  if (setsockopt(s, SOL_SOCKET, SO_RCVTIMEO,&tv,sizeof(tv)) < 0) {
    perror("Error");
  }

  memset(messageString,0,MAX);
  pmT = mmap (NULL, sizeof *pmT, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  paT = mmap (NULL, sizeof *paT, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  nsT = mmap (NULL, sizeof *nsT, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  started = mmap (NULL, sizeof *started, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);

  if(fork()==0){
    while(1){
      //write(1,"recfrom:\n",9);

      n = recvfrom(s, messageString, MAX, 0,(struct sockaddr *)&server, &len);
      printf("%s %d\n",inet_ntoa(server.sin_addr),len );

      message = processData(messageString);      
      *pmT = message.pm;
      *paT = message.pa;
      *nsT = message.ns;
      printf("while (1) %d | %d %d %d\n", message.tp, message.pm, message.pa, message.ns);

      printf("\n");
      fflush(stdout);
      if(n<0){perror("recvfrom");}
      //message = processData(messageString);
      
      printf("%d\n", messageString[0]);
      fflush(stdout);
    }
  }else{
    fp = fopen(L_DATA, "r");
    
    while(1){
      if(*started == true){
        //write(1,"enviar\n",7);
        message.pm = *pmT;
        message.pa = *paT;
        message.ns = *nsT;
        message.ts = time(NULL);
        printf("before reading %d | %d %d %d\n", message.tp, message.pm, message.pa, message.ns);
        message = readData(fp,s,message);
        printf("started %d | %d %d %d\n", message.tp, message.pm, message.pa, message.ns);

        messageString = structToString(message);
        printf("data %d %d %d %d %d %d %d\n", messageString[0],messageString[1],messageString[2],messageString[3],messageString[4],messageString[5],messageString[6]);
        //write(1,"enviar1\n",8);
        printf("before sending\n");
        if (message.ns > 0) {
          n = sendto(s, messageString, 28 + 14 * message.ns + 1, 0,(struct sockaddr *)&server, sizeof(server)); 
          if(n<0){ perror(" error sendto");}
          printf("after sending %d, type %d\n", n, messageString[0]);
          mSeconds = message.pm*1000 - message.pa*message.ns;
          printf("sleeping for %d (%d*1000 - %d*%d) ms\n", mSeconds, message.pm, message.pa, message.ns);
          usleep(mSeconds);     
        }
      }
    }   
  }
  /* Deallocate the socket */
  close(s);
  return 0;
}