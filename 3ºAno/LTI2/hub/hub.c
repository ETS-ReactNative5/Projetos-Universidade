#include <arpa/inet.h>
#include <errno.h>
#include <ifaddrs.h>
#include <limits.h>
#include <math.h>
#include <net/if.h>
#include <netdb.h> 
#include <netinet/in.h> 
#include <poll.h>
#include <signal.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define MAX 2048
#define MAX_S 32

#define U_G 16384
#define U_DPS 131
#define U_DCA 340
#define U_DCB 35

#define MIN_G -2.1
#define MAX_G 2.1
#define MIN_DPS -255
#define MAX_DPS 255
#define MIN_DC -42
#define MAX_DC 87

#define MIN_PM 50 // ms
#define MAX_PM 3600000 // = 1 h
#define MIN_PA 1 // μs
#define MAX_PA 60000000 // = 1 min
#define MIN_NS 1
#define MAX_NS 30

#define T_DEFAULT -4
#define T_SETUP -3
#define T_ERROR -2
#define T_COMS 1
#define T_ACCEPT 2
#define T_START 3
#define T_DATA 4
#define T_STOP 5
#define T_LIST 6
#define T_SHOW 7
#define T_HIDE 8
#define T_HIDE2 9
#define T_MENU 10
#define T_WSERV 11
#define T_CSERV 12
#define T_LIVE 13
#define T_USER 14

#define DB_SIMUL "simuldb.txt"

#define F_SETUP "setup.cfg"
#define F_USERS "users.cfg"

#define L_SETERR "error.log"
#define L_ERROR "error.log"
#define L_SETUP "setup.log"
#define L_INPUT "input.log"
#define L_DATA "data.log"

#define E_NOSETUP 1
#define E_COMNOTFOUND 2
#define E_INVALCOM 3
#define E_INVALDATA 4
#define E_INABILITY 5
#define E_NOANSWER 6
#define E_OPENSOCKET 7
#define E_SENDTOSS 8
#define E_BINDING 9
#define E_INVALPARAM 10
#define E_CONNECT 11
#define E_ABORTED 12
#define E_ACCEPTING 13
#define E_LISTENING 14
#define E_SENDTOSERV 15

const char *erString[] = {
	"ficheiro de configuração inexistente",							            // E_NOSETUP
	"tipo de comunicação não encontrado",							              // E_COMNOTFOUND
	"tipo de comunicação inválido",									                // E_INVALCOM
	"dados inválidos",												                      // E_INVALDATA
	"incapacidade de enviar mensagens de dados no ritmo definido",  // E_INABILITY
	"falta de resposta do sistema sensor",							            // E_NOANSWER
	"falha na abertura do socket",									                // E_OPENSOCKET
	"falha no envio da mensagem para o sistema sensor",				      // E_SENDTOSS
	"falha no binding",												                      // E_BINDING
	"parâmetros inválidos",											                    // E_INVALPARAM
	"falha na conexão com o gestor de serviço",						          // E_CONNECT
	"ligação com concentrador terminada abruptamente",				      // E_ABORTED
	"falha na conexão com concentrador",							              // E_ACCEPTING
	"falha na escuta",												                      // E_LISTENING
  "falha no envio da mensagem para o gestor de serviços",				  // E_SENDTOSERV
};

typedef char byte;

struct setup {
  char serviceIp[25];
  unsigned int servicePort;
  unsigned int hubPort;
};

int serviceSock;

struct setup setup;

int timeout = 0;
struct sockaddr_in *sensorAddr;  /* client addr */
struct sockaddr_in tempAddr;  /* client addr */

int messageSize = 0;

struct error {
  uint32_t ts;
  uint8_t iss;
  int8_t er;
};

struct conn {
  uint8_t iss;
};

struct start {
  uint32_t ts;
  uint32_t iss;
  uint32_t pm;
  uint32_t pa;
  uint8_t ns;
};

struct data {
  uint32_t ts;
  uint8_t iss;
  uint32_t pm;
  uint32_t pa;
  uint8_t ns;
  float sax[MAX_NS];
  float say[MAX_NS];
  float saz[MAX_NS];
  float sgx[MAX_NS];
  float sgy[MAX_NS];
  float sgz[MAX_NS];
  float st[MAX_NS];
};

struct stop {
  uint32_t ts;
  uint8_t iss;
  char sr[MAX_S];
};

struct error error;
struct conn conn;
struct start start;
struct data data;
struct stop stop;

struct errorS {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  int8_t er;
};

struct connS {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
};

struct startS {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  uint32_t pm;
  uint32_t pa;
  uint8_t ns;
};

struct dataS {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  uint32_t pm;
  uint32_t pa;
  uint8_t ns;
  int16_t sax[MAX_NS];
  int16_t say[MAX_NS];
  int16_t saz[MAX_NS];
  int16_t sgx[MAX_NS];
  int16_t sgy[MAX_NS];
  int16_t sgz[MAX_NS];
  int16_t st[MAX_NS];
};

struct stopS {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  char sr[MAX_S];
};

char *tStamp(time_t uTime) {
	char *tsString;
	tsString = malloc(MAX_S);
	strftime(tsString, MAX_S, "%Y-%m-%d %X", localtime(&uTime));
	return tsString;
}

int getIss(unsigned int ad) {
  int i = 0;
  for (i = 1; i < 256; i ++) {
    if (ad == (unsigned int) inet_addr(inet_ntoa(tempAddr.sin_addr))) {
      break;
    }
  }
  return i;
}

void userOutModule (int messageType, char ipString[]) {
  int a = 0;
  char issString[64], tsString[32], tsiString[32], tsfString[32], pmString[32], paString[32], nsString[32];
  struct in_addr adIp;
  //printf("%x\n", message.ad);
  
  /*strftime(tsString, 32, "%Y-%m-%d %X", localtime(&message.ts));
  strftime(tsiString, 32, "%Y-%m-%d %X", localtime(&message.tsi));
  strftime(tsfString, 32, "%Y-%m-%d %X", localtime(&message.tsf));*/

  switch (messageType) {
    case T_ERROR:
      if (error.iss == 0) {
				strcpy(issString, "<HUB>");
			}
      else {
        //adIp.s_addr = error.ad;
        sprintf(issString, "[%d] | IP: %s", error.iss, inet_ntoa(sensorAddr[error.iss].sin_addr));
      }
      printf("> %s | ERROR (%d) %s - %s;\n", tStamp(error.ts), error.er, issString, erString[error.er]);
      break;
    case T_SETUP:
      if (start.pm > 0) {
				sprintf(pmString, "%d ms", start.pm);
			}
			else {
				strcpy(pmString, "<DEFAULT>");
			}
			if (start.pa > 0) {
				sprintf(paString, "%d μs", start.pa);
			}
			else {
				strcpy(paString, "<DEFAULT>");
			}
			if (start.ns == 1) {
				sprintf(nsString, "%d amostra", start.ns);
			}
			else if (start.ns > 0) {
				sprintf(nsString, "%d amostras", start.ns);
			}
			else {
				strcpy(nsString, "<DEFAULT>");
			}
      printf("> %s | Concentrador inicializado | PM = %s | PA = %s | NS = %s;\n", tStamp(time(NULL)), pmString, paString, nsString); 
      break;
    case T_ACCEPT:
      printf("> %s | Conexão estabelecida com [%d] | UDP/IP: %s;\n", tStamp(time(NULL)), conn.iss, inet_ntoa(sensorAddr[conn.iss].sin_addr));
      printf("  Escreva \"st\"/\"start\" para iniciar a recolha de dados;\n");
      break;
    case T_START:
      if (start.iss == 0) {
				strcpy(issString, "<ALL>");
			}
			else {
        sprintf(issString, "[%d] | IP: %s", start.iss, inet_ntoa(sensorAddr[start.iss].sin_addr));
			}
      if (start.pm > 0) {
				sprintf(pmString, "%d ms", start.pm);
			}
			else {
				strcpy(pmString, "<DEFAULT>");
			}
			if (start.pa > 0) {
				sprintf(paString, "%d μs", start.pa);
			}
			else {
				strcpy(paString, "<DEFAULT>");
			}
			if (start.ns == 1) {
				sprintf(nsString, "%d amostra", start.ns);
			}
			else if (start.ns > 0) {
				sprintf(nsString, "%d amostras", start.ns);
			}
			else {
				strcpy(nsString, "<DEFAULT>");
			}
      printf("> %s | START %s - PM = %s | PA = %s | NS = %s;\n", tStamp(start.ts), issString, pmString, paString, nsString);
      break;
    case T_DATA:
      if (data.iss > 0) {
        printf("> %s | DATA [%d] | IP: %s - PM = %d ms | PA = %d μs | NS = %d amostra(s):", tStamp(data.ts), data.iss, ipString, data.pm, data.pa, data.ns);
        for (int n = 0; n < data.ns; n ++) {
          printf("\n* S%d: (% .2f, % .2f, % .2f) g, (% 3.2f, % 3.2f, % 3.2f) º/s, % 2.2f ºC", n + 1, data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
          if (n < data.ns - 1) {
            printf(",");
          }
        }
        printf(";\n");
      }
      break;
    case T_STOP:
      if (stop.iss == 0) {
				strcpy(issString, "<ALL>");
			}
			else {
        sprintf(issString, "[%d] | IP: %s", stop.iss, inet_ntoa(sensorAddr[stop.iss].sin_addr));
			}
      printf("> %s | STOP %s - %s;\n", tStamp(stop.ts), issString, stop.sr);
      break;
    /*case T_LIST:
      printf("> Conexões - Gestor de serviço: TCP/IP: %s:%d:", tsString, setup.serviceIp, setup.servicePort);
      for (a = 1; a < 256; a ++) {        
        if (sensorAddr[a] != NULL) {
          printf("\n* [%d] | IP: %s", a, inet_ntoa(sensorAddr[a].sin_addr));
        }
      }
      printf(";\n");*/
      break;
    /* case T_SHOW:
      if (message.tsi != -1 || message.tsf != -1) {
        printf("> %s | Apresentação de mensagens de dados entre %s e %s:\n", tsString, tsiString, tsfString);
      }
      else {
        printf("> %s | Apresentação de mensagens de dados em tempo real;\n", tsString);
      }
      break;
    case T_HIDE:
      printf("> %s | Ocultação de mensagens de dados em tempo real | Mensagens de erro: ", tsString);
      if (message.se == true) {
        printf("visíveis;\n");
      }
      else {
        printf("ocultas;\n");
      }
      printf(" Escreva \"sh\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
      break;
    case T_HIDE2:
      printf(" Escreva \"sh\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
      break;*/
    default:
      //printf("> TP: OTHER | TS: %s;\n", tsString);
      break;
  }
}

void manageWriteModule (int messageType) {
  FILE *fp;
  char issString[64], pmString[32], paString[32], nsString[32];
  struct in_addr adIp;
  //printf("storing %d\n", message.tp);
  switch (messageType) {
    case T_ERROR:
      fp = fopen(L_ERROR, "a");
			if (error.iss > 0) {
				sprintf(issString, "%d %s", error.iss, inet_ntoa(sensorAddr[error.iss].sin_addr));
			}
			else {
				strcpy(issString, "HUB");
			}
      fprintf(fp, "%s ERROR (%d) %s\n", tStamp(error.ts), error.er, issString);
      fclose(fp);
      break;
    case T_SETUP:
      fp = fopen(L_SETUP, "a");
			if (start.pm > 0) {
				sprintf(pmString, "%d", start.pm);
			}
			else {
				strcpy(pmString, "DEFAULT");
			}
			if (start.pa > 0) {
				sprintf(paString, "%d", start.pa);
			}
			else {
				strcpy(paString, "DEFAULT");
			}
			if (start.ns > 0) {
				sprintf(nsString, "%d", start.ns);
			}
			else {
				strcpy(nsString, "DEFAULT");
			}
      fprintf(fp, "%s SETUP %d %d %d %s:%d %d\n", tStamp(time(NULL)), start.pm, start.pa, start.ns, setup.serviceIp, setup.servicePort, setup.hubPort);
      fclose(fp);
      break;
    case T_ACCEPT:
      fp = fopen(L_SETUP, "a");
      fprintf(fp, "%s CONNECTION %d %s\n", tStamp(time(NULL)), conn.iss, inet_ntoa(sensorAddr[conn.iss].sin_addr));
      fclose(fp);
      break;
    case T_START:
      fp = fopen(L_INPUT, "a");
      if (start.iss > 0) {
				sprintf(issString, "%d %s", start.iss, inet_ntoa(sensorAddr[start.iss].sin_addr));
			}
			else {
				strcpy(issString, "ALL");
			}
			if (start.pm > 0) {
				sprintf(pmString, "%d", start.pm);
			}
			else {
				strcpy(pmString, "DEFAULT");
			}
			if (start.pa > 0) {
				sprintf(paString, "%d", start.pa);
			}
			else {
				strcpy(paString, "DEFAULT");
			}
			if (start.ns > 0) {
				sprintf(nsString, "%d", start.ns);
			}
			else {
				strcpy(nsString, "DEFAULT");
			}
      fprintf(fp, "%s START %s %s %s %s\n", tStamp(start.ts), issString, pmString, paString, nsString);
      fclose(fp);
      fp = fopen(F_SETUP, "w");
      fprintf(fp, "%d %d %d\n%s:%d\n%d", start.pm, start.pa, start.ns, setup.serviceIp, setup.servicePort, setup.hubPort);
      fclose(fp);
      break;
    case T_DATA:
      fp = fopen(L_DATA, "a");
      fprintf(fp, "%s DATA %d %s %d %d %d|", tStamp(data.ts), data.iss, inet_ntoa(sensorAddr[data.iss].sin_addr), data.pm, data.pa, data.ns);
      for (int n = 0; n < data.ns; n ++) {
					fprintf(fp, "%.2f %.2f %.2f %.2f %.2f %.2f %.2f", data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
					if (n < data.ns - 1) {
						fprintf(fp, "|");
					}
				}
				fprintf(fp, "\n");
      fclose(fp);
      break;
    case T_STOP:
      fp = fopen(L_INPUT, "a");
      if (stop.iss > 0) {
				sprintf(issString, "%d %s", stop.iss, inet_ntoa(sensorAddr[stop.iss].sin_addr));
			}
			else {
				strcpy(issString, "ALL");
			}
			fprintf(fp, "%s STOP %s %s\n", tStamp(stop.ts), issString, stop.sr);
      fclose(fp);
      break;
    default:
      break;
  }
}

void bootModule () {
  FILE *fp;
  char *buffer = NULL, pmString, paString, nsString;
  size_t len = 0;

  if (!(fp = fopen(F_SETUP, "r"))) {
    error.ts = time(NULL);
    error.er = E_NOSETUP;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    perror("");
    exit(1);
  }
  getline(&buffer, &len, fp);
  
  sscanf(buffer, "%d %d %hhd", &start.pm, &start.pa, &start.ns);
  if (start.pm > 0 && (start.pm < MIN_PM || start.pm > MAX_PM) || start.pa > 0 && (start.pa < MIN_PA || start.pa > MAX_PA) || start.ns > 0 && (start.ns < MIN_NS || start.ns > MAX_NS)) {
    error.ts = time(NULL);
    error.er = E_INVALPARAM;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    fclose(fp);
    exit(1);
  }
  if (start.pm * 1000 < start.pa * start.ns && start.pm != 0 && start.pa != 0 && start.ns != 0) {
    error.ts = time(NULL);
    error.er = E_INVALPARAM;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    fclose(fp);
    exit(1);
  }
  if (getline(&buffer, &len, fp) <= 0) {
    error.ts = time(NULL);
    error.er = E_COMNOTFOUND;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    fclose(fp);
    exit(1);
  }
  strcpy(setup.serviceIp, strtok(buffer, ":"));
  setup.servicePort = atoi(strtok(NULL, "\n"));

  if (getline(&buffer, &len, fp) <= 0) {
    error.ts = time(NULL);
    error.er = E_COMNOTFOUND;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    fclose(fp);
    exit(1);
  }
  setup.hubPort = atoi(buffer);
  fclose(fp);
}

void readLog (int iss, time_t tsi, time_t tsf) {
	int tempIsu = 0, count = 0;
	char areaLine[256], tsString[32], tsiString[32], tsfString[32], status1[32], status2[32], tempArea2[32], areaString[32], ipString[15];
	char *token;
  struct tm info;
	time_t tempTs = 0;
	FILE *fp;

	fp = fopen (L_DATA, "r");
	if (iss == 0) {
		printf("> Comportamentos de %s até %s:\n", tStamp(tsi), tStamp(tsf));
	}
	else {
		printf("> Comportamentos de [%d] de %s até %s:\n", iss, tStamp(tsi), tStamp(tsf));
	}
	if (fp != NULL) {
    data.ts = 0;
		for (int i = 0; fgets(areaLine, MAX, fp) && data.ts <= tsf; i ++) {
			if (areaLine[strlen(areaLine) - 1] == '\n') {
				areaLine[strlen(areaLine) - 1] = '\0';
			}
			memset(tempArea2, 0, sizeof(tempArea2));
			sscanf(areaLine, "%d-%d-%d %d:%d:%d DATA %hhd %s %d %d %hhd", &info.tm_year, &info.tm_mon, &info.tm_mday, &info.tm_hour, &info.tm_min, &info.tm_sec, &data.iss, ipString, &data.pm, &data.pa, &data.ns);
      //sscanf(messageString, "%hhd %ld %hhd %d %d %hhd|", &message.tp, &message.ts, &message.iss, &message.pm, &message.pa, &message.ns);
      token = strtok(areaLine, "|");
      //printf("test %s\n", token);
      for (int n = 0; (token != NULL && n < data.ns); n ++) {
        token = strtok(NULL, "|");
        //printf("test %s\n", token);
        sscanf(token, "%f %f %f %f %f %f %f", &data.sax[n], &data.say[n], &data.saz[n], &data.sgx[n], &data.sgy[n], &data.sgz[n], &data.st[n]);
      }
      //2019-06-13 16:13:36 DATA 2 127.0.0.1 2000 4000 5 | -0.03 -0.04 0.96 -4.03 0.15 0.05 25.12 | 0.01 0.01 1.00 -3.92 -3.97 -3.92 25.12 | -0.03 -0.03 1.01 0.21 -4.05 0.15 25.07 | -0.04 -0.03 1.00 0.07 -4.02 -3.98 26.72 | 0.00 0.00 0.97 0.15 -3.98 0.10 25.07
			info.tm_year -= 1900;
			info.tm_mon --;
			data.ts = mktime(&info);
			if (data.ts >= tsi && data.ts <= tsf && (iss == 0 || iss == data.iss)) {
        userOutModule(T_DATA, ipString);    
        count ++;
			}
		}
		fclose(fp);
	}
	printf("* %d resultados;\n", count);
  write(1, "debug0\n", 7);
}

/* void printData () {
  int n = 0;
  char messageString[MAX];
  struct message tempMessage;
  FILE *fp;

  tempMessage.ts = 0;
  
  if (fp = fopen(L_DATA, "r")) {
    while (fgets(messageString, MAX, fp) && tempMessage.ts <= show.tsf) {
    	messageString[0] = T_SHOW;
      tempMessage = stringToStruct(messageString);
      if (tempMessage.ts >= message.tsi && tempMessage.ts <= message.tsf) {
        userOutModule(tempMessage);
        n ++;
      }
    }
  }
  if (message.tsi != -1 || message.tsf != -1) {
    printf("> TOTAL: %d mensagem/ns;\n", n);
  }
  fclose(fp);
}*/

void printMenu() {
	printf("> Menu de comandos disponíveis:\n");
  printf("* st/start <ISS> <PM (ms) ∈ [%d, %d]> <PA (μs) ∈ [%d, %d]> <NS ∈ [%d, %d]> - iniciar recolha de dados,\n", MIN_PM, MAX_PM, MIN_PA, MAX_PA, MIN_NS, MAX_NS);
  printf("* s/stop <ISS> <SR> - parar recolha de dados,\n");
  printf("* n/net - mostrar sistemas sensores e gestor de serviço connectados,\n");
  printf("* sh/show <TSI (Y-M-D H:M:S)> <TSF (Y-M-D H:M:S)> - mostrar mensagens recebidas num intervalo de tempo,\n");
  printf("* sh/show - mostrar mensagens de dados recebidas em tempo real,\n");
  printf("* h/hide - ocultar mensagens de dados recebidas,\n");
  printf("* m/menu - mostrar o menu;\n");
}

int listUsers(unsigned char isuList[], unsigned char isu[]) {
	int i, max = 0;
  unsigned char tempIsu;
	char *userLine, *token;  
  userLine = malloc(MAX);

  size_t len = sizeof(userLine);
	FILE *fp;

  memset(userLine, 0, 1024);
  memset(isuList, 0, 256);
  memset(isu, 0, 256);
  

  printf("line: %s\n", userLine);

	fp = fopen (F_USERS, "r");
	for (i = 0; getline(&userLine, &len, fp) > 0; i ++) {
    //printf("line: %s\n", userLine);
		if (userLine[strlen(userLine) - 1] == '\n') {
			userLine[strlen(userLine) - 1] = '\0';
		}
		token = strtok(userLine, " ");
    tempIsu = atoi(token);
    isuList[i] = tempIsu;
    //printf("isuList[%d] = #%d\n", i, tempIsu);
		while (token != NULL) {
			token = strtok(NULL, " ");
			if (token != NULL) {
        isu[atoi(token)] = tempIsu;
        //printf("isu[%d] = #%d\n", atoi(token), tempIsu);
        if (atoi(token) > max) {
          max = atoi(token);
        }
			}
		}
	}
	printf("> Lista de sujeitos (#ISu, [ISS]):\n");
	for (int j = 0; j < i; j ++) {
		printf("* #%d", isuList[j]);
		for (int k = 1; k <= max; k ++) {
      if (isu[k] == isuList[j]) {
				printf(" [%d]", k);
			}
		}
		printf("\n");
	}
	fclose(fp);
	return max;
}

void recUsers(unsigned char isuList[]) {
  char messageType = T_USER;

  memset(isuList, 0, 256);
  send(serviceSock, &messageType, sizeof(messageType), 0);
  recv(serviceSock, isuList, 256, 0);
}

int setUser(unsigned char isuList[], unsigned char isu[], unsigned char iss, unsigned char tempIsu) {
	int found = 0;
	FILE *fp;

  recUsers(isuList);
	
	fp = fopen (F_USERS, "w");
	fclose(fp);
	fp = fopen (F_USERS, "a");

	/*if (iss > max) {
		max = iss;
  }*/

  isu[iss] = tempIsu;
	for (int i = 0; isuList[i] > 0; i ++) {
		fprintf(fp, "%d", isuList[i]);
		for (int j = 1; j < 256; j ++) {
			if (isu[j] > 0 && isu[j] == isuList[i]) {
				if (j == iss) {
					found = iss;
				}
        isu[j] = isuList[i];
				fprintf(fp, " %d", j);
			}
		}
		fprintf(fp, "\n");
	}
	if (found == 0) {
		isu[iss] = 0;
	}
	//printf("area[%d] = %s\n", isu, area[isu]);
	fclose(fp);
	return found;
}

void getUsers(unsigned char isuList[], unsigned char isu[]) {
	int i, max = 0;
  unsigned char tempIsu;
	char userLine[1024], *token;
	FILE *fp;
  isu[0] = 0;

  memset(isuList, 0, 256);
  memset(isu, 0, 256);
	fp = fopen (F_USERS, "r");
	for (i = 0; fgets(userLine, MAX, fp); i ++) {
		if (userLine[strlen(userLine) - 1] == '\n') {
			userLine[strlen(userLine) - 1] = '\0';
		}
		token = strtok(userLine, " ");
    tempIsu = atoi(token);
    isuList[i] = tempIsu;
		while (token != NULL) {
			token = strtok(NULL, " ");
			if (token != NULL) {
        isu[atoi(token)] = tempIsu;
        if (atoi(token) > max) {
          max = atoi(token);
        }
			}
		}
	}
  printf("got users\n");
	fclose(fp);
}

int userInModule () {
  int messageType, tempTp = 0, a = 0, isu;
  time_t tsi = 0, tsf = time(NULL);
  char *token, tsString[20], buffer[MAX];
  unsigned char isuList[256], tempIsu[256];
  bool error = false, found = false;
  struct tm info;

  tempTp = messageType;
  messageType = T_DEFAULT;
  getUsers(isuList, tempIsu);

  while (messageType != T_START && messageType != T_STOP && messageType != T_LIST && messageType != T_SHOW && messageType != T_HIDE && messageType != T_MENU || error == true) {
  	error = false;
    found = false;
    fgets(buffer, MAX, stdin);
    if (strlen(buffer) <= 1) {
      strcpy(buffer, "eof\n");
    }
    buffer[strlen(buffer) - 1] = ' ';
    token = strtok(buffer, " ");
    if (!strcasecmp(token, "start") || !strcasecmp(token, "st")) {
      messageType = T_START;
      start.ts = time(NULL);
      start.iss = 0;
      if ((token = strtok(NULL, " ")) != NULL) {
        start.iss = atoi(token);
        if (inet_addr(inet_ntoa(sensorAddr[start.iss].sin_addr)) > 0) {
          found = true;
        }
        if (!found && start.iss != 0) {
          error = true;
        }
      }
      if ((token = strtok(NULL, " ")) != NULL) {
        start.pm = atol(token);
        if (start.pm > 0 && (start.pm < MIN_PM || start.pm > MAX_PM)) {
        	error = true;
        }
      }
      if ((token = strtok(NULL, " ")) != NULL) {
        start.pa = atol(token);
        if (start.pa > 0 && (start.pa < MIN_PA || start.pa > MAX_PA)) {
        	error = true;
        }
      }
      if ((token = strtok(NULL, " ")) != NULL) {
        start.ns = atoi(token);
        if (start.ns > 0 && (start.ns < MIN_NS || start.ns > MAX_NS) || (start.pm > 0 || start.pa > 0 || start.pm > 0) && start.pm * 1000 < start.pa * start.ns) {
        	error = true;
        }
      }
      if (error == true) {
      	printf("> parâmetros inválidos;\n");
      }
    }
    else if (!strcasecmp(token, "stop") || !strcasecmp(token, "s")) {
      messageType = T_STOP;
      stop.ts = time(NULL);
      stop.iss = 0;
      if ((token = strtok(NULL, " ")) != NULL) {
        stop.iss = atol(token);
        if (inet_ntoa(sensorAddr[stop.iss].sin_addr) != NULL) {
          found = true;
        }
        if (!found && stop.iss != 0) {
          error = true;
        }
      }
      if ((token = strtok(NULL, "\n")) != NULL && strlen(token) > 1) {
        token[strlen(token) - 1] = '\0';
        sprintf(stop.sr, "USERIN(%s)", token);
      }
      else {
        strcpy(stop.sr, "USERIN");
      }
      if (error == true) {
      	printf("> parâmetros inválidos;\n");
      }
    }
    else if (!strcasecmp(token, "show") || !strcasecmp(token, "sh")) {
      messageType = T_SHOW;
      if ((token = strtok(NULL, " ")) != NULL) {
        if (strchr(token, '-') == NULL) {
          isu = atoi(token);          
          token = strtok(NULL, " ");
        }
        else {
          isu = 0;
        }
        sscanf(token, "%d-%d-%d", &info.tm_year, &info.tm_mon, &info.tm_mday);
        info.tm_year -= 1900;
        info.tm_mon --;
        if ((token = strtok(NULL, " ")) != NULL) {
          sscanf(token, "%d:%d:%d", &info.tm_hour, &info.tm_min, &info.tm_sec);
          tsi = mktime(&info);
        }
        if ((token = strtok(NULL, " ")) != NULL) {         
          sscanf(token, "%d-%d-%d", &info.tm_year, &info.tm_mon, &info.tm_mday);
          info.tm_year -= 1900;
          info.tm_mon --;
          if ((token = strtok(NULL, " ")) != NULL) {
            sscanf(token, "%d:%d:%d", &info.tm_hour, &info.tm_min, &info.tm_sec);
            tsf = mktime(&info);
          }
        }
        else {
          tsf = time(NULL);
        }
        readLog(isu, tsi, tsf);
        write(1, "debug1\n", 7);
      }
      else {
        messageType = T_LIVE;
        printf("> A mostrar mensagens em tempo real.");
      }
    }
    else if (!strcasecmp(token, "hide") || !strcasecmp(token, "h")) {
      messageType = T_HIDE;
    }
    else if (!strcasecmp(token, "list") || !strcasecmp(token, "l")) {
      messageType = T_LIST;
      recUsers(isuList);
      setUser(isuList, tempIsu, 0, 0);
    }
    else if (!strcasecmp(token, "user") || !strcasecmp(token, "u")) {
      messageType = T_USER;
      if ((token = strtok(NULL, " ")) != NULL) {
        isu = atoi(token);
        while (token != NULL) {
          token = strtok(NULL, " ");
          if (token != NULL) {														
            //setUser(isuList, isu, tempIsu, tempArea);
            printf("setting [%d] to #%d\n", atoi(token), isu);
            setUser(isuList, tempIsu, atoi(token), isu);
          }
        }        
        listUsers(isuList, tempIsu);
        //max = listUsers(areaList, area);
      }
    }
    else if (!strcasecmp(token, "menu") || !strcasecmp(token, "m")) {
      messageType = T_MENU;
      printMenu();
    }    
    else {
      printf("> entrada inválida;\n");
    }
  }
  write(1, "debug2\n", 7);
  return messageType;
}

unsigned char *structToString (int messageType) {
  unsigned char messageString[MAX];
  unsigned char *messageStringT;
  memset(messageString, 0, MAX);
  //memset(messageStringT, 0, MAX);
  messageString[0] = messageType;
  switch (messageType) {
    case T_START: //START

      //printf("ts %ld\n", message.ts);
      messageString[1] = start.ts >> 24;
      messageString[2] = start.ts >> 16;
      messageString[3] = start.ts >> 8;
      messageString[4] = start.ts;

      //printf("pm %d\n", message.pm);
      messageString[5] = start.pm >> 24;
      messageString[6] = start.pm >> 16;
      messageString[7] = start.pm >> 8;
      messageString[8] = start.pm;

      //printf("pa %d\n", message.pa);
      messageString[9] = start.pa >> 24;
      messageString[10] = start.pa >> 16;
      messageString[11] = start.pa >> 8;
      messageString[12] = start.pa;

      //printf("ns %d\n", message.ns);
      messageString[13] = start.ns;

      messageSize = 14;

      /*for (int i = 0; i < messageSize; i ++) {
        printf("%x ", messageString[i]);
      }
      printf("\n");*/
      //sprintf(messageString, "%d %d %d %d %d", message.tp, message.ts, message.pm, message.pa, message.ns);
      break;
    case T_STOP: //STOP

      messageString[1] = stop.ts >> 24;
      messageString[2] = stop.ts >> 16;
      messageString[3] = stop.ts >> 8;
      messageString[4] = stop.ts;

      strcat(messageString, stop.sr);

      messageSize = 5 + strlen(stop.sr);
      
      //sprintf(messageString, "%d %d %s", message.tp, message.ts, message.sr);
      break;
    default: //OTHER
      messageSize = 1;
      //sprintf(messageString, "%d", message.tp);
      break;
  }
  messageStringT = messageString;
  return messageStringT;
}

int stringToStruct (char messageString[]) {
  int a = 0, messageType;
  int saxTemp = 0, sayTemp = 0, sazTemp = 0, sgxTemp = 0, sgyTemp = 0, sgzTemp = 0, stTemp = 0, found = 0;
  //char messageStringT[MAX], *token;

  FILE *fp;
  
  //strcpy(messageStringT, messageString);
  //token = strtok(messageStringT, " ");
  //printf("sts ");
  for (int i = 0; i < messageSize; i ++) {
    messageString[i] = (unsigned char) messageString[i];
    //printf("%x ", messageString[i]);
  }
  //printf("\n");
  messageType = messageString[0];

  switch (messageType) {
  	case T_COMS: //ERROR
      conn.iss = messageString[1];
      sensorAddr[conn.iss] = tempAddr;
      //sscanf(messageString, "%hhd %d %d", &message.tp, &message.ad, &message.iss);
      //printf("S2s: %x",message.ad);
      break;
    case T_ERROR: //ERROR
      error.ts = (unsigned char) messageString[1] * pow(2, 24) + (unsigned char) messageString[2] * pow(2, 16) + (unsigned char) messageString[3] * pow(2, 8) + (unsigned char) messageString[4];
      error.iss = messageString[5];
      sensorAddr[error.iss] = tempAddr;
      error.er = messageString[6];

      /*for (int a = 1; a < 255 && found == 0; a ++) {
        if (message.ad == inet_addr(inet_ntoa(sensorAddr[a].sin_addr))) {
          message.iss = a;
          found = 1;
        }
      }*/
      found = 0;
      //sscanf(messageString, "%hhd %d %d %hhd", &message.tp, &message.ts, &message.ad, &message.er);
      break;
    case T_DATA: //DATA
      fp = fopen(DB_SIMUL, "a");
      data.ts = (unsigned char) messageString[1] * pow(2, 24) + (unsigned char) messageString[2] * pow(2, 16) + (unsigned char) messageString[3] * pow(2, 8) + (unsigned char) messageString[4];
      data.iss = messageString[5];
      sensorAddr[data.iss] = tempAddr;
      data.pm = (unsigned char) messageString[6] * pow(2, 24) + (unsigned char) messageString[7] * pow(2, 16) + (unsigned char) messageString[8] * pow(2, 8) + (unsigned char) messageString[9];
      //printf("pm %d %x\n", message.pm, message.pm);
      data.pa = (unsigned char) messageString[10] * pow(2, 24) + (unsigned char) messageString[11] * pow(2, 16) + (unsigned char) messageString[12] * pow(2, 8) + (unsigned char) messageString[13];
      //printf("pa %d %x\n", message.pa, message.pa);
      data.ns = (unsigned char) messageString[14];
      //printf("ns %d %x\n", message.ns, message.ns);
      //sscanf(messageString, "%hhd %d %d %d %d %hhd|", &message.tp, &message.ts, &message.iss, &message.pm, &message.pa, &message.ns);
      //token = strtok(NULL, "|");
      for (int n = 0; n < data.ns && messageType != T_ERROR; n ++) {
        //token = strtok(NULL, "|");
        saxTemp = messageString[15 + 14 * n] * pow(2, 8) + messageString[16 + 14 * n];
        sayTemp = messageString[17 + 14 * n] * pow(2, 8) + messageString[18 + 14 * n];
        sazTemp = messageString[19 + 14 * n] * pow(2, 8) + messageString[20 + 14 * n];
        sgxTemp = messageString[21 + 14 * n] * pow(2, 8) + messageString[22 + 14 * n];
        sgyTemp = messageString[23 + 14 * n] * pow(2, 8) + messageString[24 + 14 * n];
        sgzTemp = messageString[25 + 14 * n] * pow(2, 8) + messageString[26 + 14 * n];
        stTemp = messageString[27 + 14 * n] * pow(2, 8) + messageString[28 + 14 * n];

        fprintf(fp, "%d %d %d %d %d %d %d\n", saxTemp, sayTemp, sazTemp, sgxTemp, sgyTemp, sgzTemp, stTemp);

        data.sax[n] = (float) (saxTemp) / U_G;
        data.say[n] = (float) (sayTemp) / U_G;
        data.saz[n] = (float) (sazTemp) / U_G;
        data.sgx[n] = (float) (sgxTemp) / U_DPS;
        data.sgy[n] = (float) (sgyTemp) / U_DPS;
        data.sgz[n] = (float) (sgzTemp) / U_DPS;
        data.st[n] = (float) (stTemp) / U_DCA + U_DCB;
        //sscanf(token, "%d %d %d %d %d %d %d", &saxTemp, &sayTemp, &sazTemp, &sgxTemp, &sgyTemp, &sgzTemp, &stTemp);
        /*message.sax[n] = (float) saxTemp / U_G;
        message.say[n] = (float) sayTemp / U_G;
        message.saz[n] = (float) sazTemp / U_G;
        message.sgx[n] = (float) sgxTemp / U_DPS;
        message.sgy[n] = (float) sgyTemp / U_DPS;
        message.sgz[n] = (float) sgzTemp / U_DPS;
        message.st[n] = (float) stTemp / U_DCA + U_DCB;*/
        if (data.sax[n] > MAX_G || data.sax[n] < MIN_G || data.say[n] > MAX_G || data.say[n] < MIN_G || data.saz[n] > MAX_G || data.saz[n] < MIN_G ||
        	data.sgx[n] > MAX_DPS || data.sgx[n] < MIN_DPS || data.sgy[n] > MAX_DPS || data.sgy[n] < MIN_DPS || data.sgz[n] > MAX_DPS || data.sgz[n] < MIN_DPS ||
        	data.st[n] > MAX_DC || data.st[n] < MIN_DC) {
        	messageType = T_ERROR;
        	error.ts = time(NULL);
          error.iss = data.iss;
        	error.er = E_INVALDATA;
        }
      }
      fclose(fp);
      break;
  	case T_SHOW:
	    //sscanf(messageString, "%hhd %ld %hhd %d %d %hhd|", &message.tp, &message.ts, &message.iss, &message.pm, &message.pa, &message.ns);
      /*token = strtok(NULL, "|");
      for (int n = 0; (token != NULL && n < message.ns); n ++) {
        token = strtok(NULL, "|");
        sscanf(token, "%f %f %f %f %f %f %f", &message.sax[n], &message.say[n], &message.saz[n], &message.sgx[n], &message.sgy[n], &message.sgz[n], &message.st[n]);
      }
      message.tp = T_DATA;*/
      break;
    default: //OTHER
      //sscanf(messageString, "%hhd", &message.tp);
      break;
  }
  return messageType;
}

int comSendModule (unsigned char messageString[], int issTemp, int hubSock) {
  char tempString[messageSize];
  int n, a = 0;
  struct in_addr adIp;
  /*printf("\neheh\n");
  printf("\nsize\n);//%ld\n", strlen(messageString));
  printf("\n hhh %x\n", messageString[0]);
  printf("\nuwu %d\n", messageSize);*/
  memcpy(tempString, messageString, messageSize);
  /*printf("sending ");
  for (int i = 0; i < messageSize; i ++) {
    printf("%d ", tempString[i]);
  }
  printf("\n");*/
  if (issTemp == 0) {
    for (a = 1; a < 255; a ++) {
      if (inet_addr(inet_ntoa(sensorAddr[a].sin_addr)) > 0) {
        //printf("> %d %d %s\n", a, issTemp, inet_ntoa(adIp));
        n = sendto(hubSock, tempString, messageSize, 0,(struct sockaddr *)&sensorAddr[a], sizeof(sensorAddr[a]));
        /*printf("\nowo %d\n", n);
        printf("\n~ %s\n", inet_ntoa(sensorAddr.sin_addr));*/
        if (n < 0) {
          error.ts = time(NULL);
          error.iss = issTemp;
          error.er = E_SENDTOSS;
          manageWriteModule(T_ERROR);
          userOutModule(T_ERROR, NULL);
          //perror("ERROR in sendto");
        }
      }
    }
  }
  else {
    n = sendto(hubSock, tempString, messageSize, 0,(struct sockaddr *)&sensorAddr[issTemp], sizeof(sensorAddr[issTemp]));
    /*printf("\nowo %d\n", n);
    printf("\n~ %s\n", inet_ntoa(sensorAddr.sin_addr));*/
    if (n < 0) {
      error.ts = time(NULL);
      error.iss = issTemp;
      error.er = E_SENDTOSS;
      manageWriteModule(T_ERROR);
      userOutModule(T_ERROR, NULL);
      perror("");
    }
  }
}

char *comReceiveModule (int hubSock) {
  int res, messageType;
  ssize_t n=0;
  char *messageString;
  messageString = malloc(MAX);

  //while(read(Arduino, messageString, MAX) && timeout == 0);
  
	  socklen_t len = sizeof(tempAddr);
	  memset(messageString,0,MAX);
    printf("waiting for sensor\n");
    //write(1, "get\n", 4);
	  n = recvfrom(hubSock, messageString, MAX, 0,(struct sockaddr *)&tempAddr, &len);    
    printf("GOT %ld, type %d\n", n, messageString[0]);
    /*printf("got ");
    for (int i = 0; i < n; i ++) {
      printf("%d ", messageString[i]);
    }
    printf("\n");*/
	  if (n < 0){
      //perror("ERROR in recvfrom");
      messageType = T_ERROR;
      error.ts = time(NULL);
      //for (a = 0; message.iss != iss[a]; a ++);      
      error.iss = getIss(inet_addr(inet_ntoa(tempAddr.sin_addr)));
      error.er = E_NOANSWER;
      messageString[0] = messageType;
      messageString[1] = error.ts >> 24;
      messageString[2] = error.ts >> 16;
      messageString[3] = error.ts >> 8;
      messageString[4] = error.ts;
      messageString[5] = error.iss;
      messageString[6] = error.er;
      //sprintf(messageString, "%hhd %ld %ld %hhd", T_ERROR, time(NULL), (long int) inet_addr(inet_ntoa(sensorAddr.sin_addr)), E_NOANSWER);
    }
	  //printf("receive %s\n", messageString);
    //printf("yeet %d\n", n);
    messageSize = n;
    /*for (int i = 0; i < n; i ++) {
      printf("%d ", (unsigned char) messageString[i]);
    }
    printf("%s\n", inet_ntoa(sensorAddr.sin_addr));*/
	  return messageString;
    //messageString = "-2 42 123456789 5";
  
  timeout = 0;
  
  return messageString;
}

void servSend (int messageType) {
  unsigned char isuList[256], isu[256];
  int tempIss;
  struct errorS errorS;
  struct connS connS;
  struct startS startS;
  struct dataS dataS;
  struct stopS stopS;
  ssize_t sentSize = 0;
  printf("in serv\n");

  getUsers(isuList, isu);
  printf("after getting\n");

  switch (messageType) {
    case T_ERROR:
      tempIss = error.iss;
      errorS.tp = T_ERROR;
      errorS.ts = error.ts;
      errorS.ad = inet_addr(inet_ntoa(sensorAddr[error.iss].sin_addr));      
      errorS.isu = isu[error.iss];      
      errorS.er = error.er;
      //printf("sending to serv: %d %d %d %d %d\n", error.tp, error.ts, error.ad, error.isu, error.er);
      sentSize = send(serviceSock, &errorS, sizeof(errorS), 0);
      break;
    case T_ACCEPT:
      tempIss = conn.iss;
      connS.tp = T_COMS;
      connS.ts = time(NULL);
      connS.ad = inet_addr(inet_ntoa(sensorAddr[conn.iss].sin_addr));
      connS.isu = isu[conn.iss];
      //printf("sending to serv: %d %d %d %d\n", conn.tp, conn.ts, conn.ad, conn.isu);
      sentSize = send(serviceSock, &connS, sizeof(connS), 0); 
      break;
    case T_START:
      tempIss = start.iss;
      startS.tp = T_START;
      startS.ts = start.ts;
      startS.ad = inet_addr(inet_ntoa(sensorAddr[start.iss].sin_addr));
      startS.isu = isu[start.iss];
      startS.pm = start.pm;
      startS.pa = start.pa;
      startS.ns = start.ns;
      //printf("sending to serv: %d %d %d %d %d %d %d\n", start.tp, start.ts, start.ad, start.isu, start.pm, start.pa, start.ns);
      sentSize = send(serviceSock, &startS, sizeof(startS), 0);
      printf("sent #%d %s [%d]\n", startS.isu, inet_ntoa(sensorAddr[start.iss].sin_addr), start.iss);
      //sentSize = sendto(serviceSock, &start, sizeof(start), 0,(struct sockaddr *)&serviceAddr, sizeof(serviceAddr)); 
      break;
    case T_DATA:
      tempIss = data.iss;
      dataS.tp = T_DATA;
      dataS.ts = data.ts;
      dataS.ad = inet_addr(inet_ntoa(sensorAddr[data.iss].sin_addr));
      dataS.isu = isu[data.iss];
      dataS.pm = data.pm;
      dataS.pa = data.pa;
      dataS.ns = data.ns;
      //printf("sending to serv: %d %d %d %d %d %d %d", data.tp, data.ts, data.ad, data.isu, data.pm, data.pa, data.ns);
      for (int n = 0; n < data.ns; n ++) {
        dataS.sax[n] = data.sax[n] * 100;
        dataS.say[n] = data.say[n] * 100;
        dataS.saz[n] = data.saz[n] * 100;
        dataS.sgx[n] = data.sgx[n] * 100;
        dataS.sgy[n] = data.sgy[n] * 100;
        dataS.sgz[n] = data.sgz[n] * 100;
        dataS.st[n] = data.st[n] * 100;
        //printf("|%d %d %d %d %d %d %d", data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
      }
      //printf("\n");
      sentSize = send(serviceSock, &dataS, sizeof(dataS), 0);
      break;
    case T_STOP:
      tempIss = stop.iss;
      stopS.tp = T_STOP;
      stopS.ts = stop.ts;
      stopS.ad = inet_addr(inet_ntoa(sensorAddr[stop.iss].sin_addr));
      stopS.isu = isu[stop.iss];
      strcpy(stopS.sr, stop.sr);
      //printf("sending to serv: %d %d %d %d %s\n", stop.tp, stop.ts, stop.ad, stop.isu, stop.sr);
      sentSize = send(serviceSock, &stopS, sizeof(stopS), 0);
      break;
    default:
      break;
  }
  printf("size %ld\n", sentSize);
  if (sentSize <= 0) {
    error.ts = time(NULL);
    error.iss = tempIss;
    error.er = E_SENDTOSERV;
    manageWriteModule(T_ERROR);
    userOutModule(T_ERROR, NULL);
    perror("perror");
  }
  //printf("sent size: %ld\n", sentSize);
}

int main () {
  int *show = 0, p[2], messageType;
  char *messageString;
  unsigned char isuList[256], isu[256];
  bool accepted = false;
  struct timeval timeout={0,0};   //timeout to recvfrom
  sensorAddr = malloc(MAX);

  int hubSock, bound;
  struct sockaddr_in serviceAddr, hubAddr;

  messageString = malloc(MAX);

  printf("> A inicializar Concentrador...\n");

  bootModule();  //SETUP
  
  hubSock = socket(AF_INET, SOCK_DGRAM, 0);
  if (hubSock <= 0) {
  	error.ts = time(NULL);
  	error.er = E_OPENSOCKET;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    perror("");
    exit(1);
  }
  printf("> %s | Socket de servidor aberto.\n", tStamp(time(NULL)));
  //==========================================Setup sockets========  
  memset(&sensorAddr, 0, sizeof(sensorAddr));
  memset(&hubAddr, 0, sizeof(hubAddr));
  memset(&serviceAddr, 0, sizeof(serviceAddr));
  hubAddr.sin_family = AF_INET;
  hubAddr.sin_addr.s_addr = INADDR_ANY;
	hubAddr.sin_port = htons(setup.hubPort);

  bound = bind(hubSock, (struct sockaddr*)&hubAddr, sizeof(hubAddr));
	if (bound < 0) {
  	error.ts = time(NULL);
  	error.er = E_BINDING;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    perror("");
    exit(1);
	}
  printf("> %s | Binding feito na porta %d.\n", tStamp(time(NULL)), setup.hubPort);

  serviceSock = socket(AF_INET, SOCK_STREAM, 0);
  if (serviceSock <= 0) {
    error.ts = time(NULL);
  	error.er = E_OPENSOCKET;
  	manageWriteModule(T_ERROR);
  	userOutModule(T_ERROR, NULL);
    perror("");
    exit(1);
  }
  printf("> %s | Socket de cliente aberto.\n", tStamp(time(NULL)));

  serviceAddr.sin_family = AF_INET;
	serviceAddr.sin_addr.s_addr = inet_addr(setup.serviceIp);
	serviceAddr.sin_port = htons(setup.servicePort);

  printf("> A estabelecer ligação com Gestor de Serviços | TCP/IP: %s:%d...\n", setup.serviceIp, setup.servicePort);
  while (connect(serviceSock, (struct sockaddr*)&serviceAddr, sizeof(serviceAddr)) < 0);
  printf("> %s | Ligação estabelecida com Gestor de Serviços.\n", tStamp(time(NULL)));
  /*perror("");*/
  manageWriteModule(T_SETUP);
  userOutModule(T_SETUP, NULL);

  show = mmap (NULL, sizeof *show, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  sensorAddr = mmap (NULL, sizeof sensorAddr, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
  
  memset(isuList, 0, 256);
  memset(isu, 0, 256);
  getUsers(isuList, isu);
  setUser(isuList, isu, 0, 0);
  listUsers(isuList, isu);
  printMenu();
  printf("> À procura de conexões de sistemas sensores...\n");
  if (fork() == 0) {  //receber
    while (1) {
      //printf("inside while type %d\n", message.tp);
      messageString = comReceiveModule(hubSock);
      messageType = stringToStruct(messageString);
        /*if (message.tp == T_ERROR && message.er == E_NOANSWER) {
          accepted = false;
        }*/
      
      //printf("aa %s\n", messageString);
      //if (*mode == 0) {
      setsockopt(hubSock,SOL_SOCKET,SO_RCVTIMEO,0,sizeof(struct timeval));
      /*}
      else {
        timeout.tv_sec = message.pm / 1000 * 2;
        setsockopt(sockfd,SOL_SOCKET,SO_RCVTIMEO,(char*)&timeout,sizeof(struct timeval));
      }*/
      if (messageType == T_COMS) {
        //printf("coms\n");
        messageString[0] = T_ACCEPT;
        messageSize = 1;
        messageType = T_ACCEPT;
        sensorAddr[conn.iss] = tempAddr;
        comSendModule(messageString, conn.iss, hubSock);
      }
        
      //if (*mode != -2) {       
      
      manageWriteModule(messageType);        
      servSend(messageType);
      if (messageType != T_DATA || *show == 1) {
        userOutModule(messageType, inet_ntoa(sensorAddr[data.iss].sin_addr));
      }
      //}
        /*messageString = comReceiveModule();
        message = stringToStruct(messageString);
        servSend(message);
        accepted = true;*/
      //}
      //printf("after coms while\n");
      /*if (poll(&(struct pollfd) {.fd = p[0], .events = POLLIN}, 1, 0) == 1) {
        read(p[0], &mode, 4);
      }*/
      /*if (*mode == 1 && message.tp != 0) {
        //printf("in show\n");
        userOutModule(message);
      }*/
      /*else if (*mode == -1 && message.tp != T_DATA) {
        userOutModule(message);
      }*/
      memset(&messageString, 0, sizeof(messageString));
    }    
    close(serviceSock);
    close(hubSock);
    exit(1);
  }
  else {         //enviar
    while (1) {
      memset(messageString, 0, sizeof(messageString));
      messageType = userInModule();
      if (messageType == T_START) {
        messageString = structToString(T_START);
        printf("sending\n");
        comSendModule(messageString, start.iss, hubSock);        
        printf("managing\n");
        manageWriteModule(T_START);
        printf("outing\n");
        userOutModule(T_START, NULL);
        printf("serving\n");
        servSend(T_START);
        *show = 1;
      }
      else if (messageType == T_STOP) {          
        messageString = structToString(T_STOP);
        comSendModule(messageString, stop.iss, hubSock);        
        manageWriteModule(T_STOP);
        userOutModule(T_STOP, NULL);
        servSend(T_STOP);
        *show = 1;
      }
      else if (messageType == T_LIVE) {
        *show = 1;
      }
      else {
        *show = 0;
      }
      //write(p[1], &mode, sizeof(mode));
    }
    close(serviceSock);
    close(hubSock);
    exit(1);
  }
  return 0;
}