#include <unistd.h> 
#include <stdio.h> 
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <stdlib.h> 
#include <math.h>
#include <netinet/in.h> 
#include <string.h> 
#include <time.h>
#include <arpa/inet.h>

#define T_DEFAULT -5
#define T_HUBDIS -4
#define T_HUBCON -3
#define T_ERROR -2
#define T_CONN 1
#define T_START 3
#define T_DATA 4
#define T_STOP 5
#define T_USER 14

#define F_SETUP "areas.cfg"
#define DB_MESSAGE "messagedb.txt"
#define DB_CONDITION "conditiondb.txt"

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

const char *erString[] = {
	"ficheiro de configuração inexistente",							// E_NOSETUP
	"tipo de comunicação não encontrado",							// E_COMNOTFOUND
	"tipo de comunicação inválido",									// E_INVALCOM
	"dados inválidos",												// E_INVALDATA
	"incapacidade de enviar mensagens de dados no ritmo definido",	// E_INABILITY
	"falta de resposta do sistema sensor",							// E_NOANSWER
	"falha na abertura do socket",									// E_OPENSOCKET
	"falha no envio da mensagem para o sistema sensor",				// E_SENDTOSS
	"falha no binding",												// E_BINDING
	"parâmetros inválidos",											// E_INVALPARAM
	"falha na conexão com o gestor de serviço",						// E_CONNECT
	"ligação com concentrador terminada abruptamente",				// E_ABORTED
	"falha na conexão com concentrador",							// E_ACCEPTING
	"falha na escuta",												// E_LISTENING
};

#define C_STOPPED "PARADO"
#define C_LYING "DEITADO"
#define C_WALKING "ANDAR"
#define C_RUNNING "CORRER"
#define C_TROUBLED "AGITADO"
#define C_FALLING "QUEDA"
#define C_INACTIVE "INATIVO"

#define MAX_NS 30
#define MAX_S 32
#define MAX 1024
#define MAX_CLIENTS 8

#define ISE 1
#define NAME "Pediatria"

#define PORT 7779
#define WEB "http://your-server-name/"

struct hubdis {
  uint32_t ad;
  uint16_t port;
};

struct hubcon {
  uint32_t ad;
  uint16_t port;
};

struct error {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  int8_t er;
};

struct conn {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
};

struct start {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  uint32_t pm;
  uint32_t pa;
  uint8_t ns;
};

struct tempData {
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

struct data {
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
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
  int8_t tp;
  uint32_t ts;
  uint32_t ad;
  uint8_t isu;
  char sr[MAX_S];
};
	
struct hubdis hubdis;
struct hubcon hubcon;
struct error error;
struct conn conn;
struct start start;
struct tempData tempData;
struct data data;
struct stop stop;

char *tStamp(time_t uTime) {
	char *tsString;
	tsString = malloc(MAX_S);
	strftime(tsString, MAX_S, "%Y-%m-%d %X", localtime(&uTime));
	return tsString;
}

char *getStatus() {
	char *status;
	float absA = 0, absG = 0, maxA = -9, minA = 9, maxG = -999, minG = 999, maxZ = -9;

	status = malloc(32);

	for (int n = 0; n < data.ns; n ++) {
		if (data.saz[n] > maxZ) {
			maxZ = data.saz[n];
		}
		absA = sqrt(pow(data.sax[n], 2) + pow(data.say[n], 2) + pow(data.saz[n], 2));
		if (absA > maxA) {
			maxA = absA;
		}
		if (absA < minA) {
			minA = absA;
		}
		absG = sqrt(pow(data.sgx[n], 2) + pow(data.sgy[n], 2) + pow(data.sgz[n], 2));
		if (absG > maxG) {
			maxG = absG;
		}
		if (absG < minG) {
			minG = absG;
		}
		//printf("\n* S%d: (% .2f, % .2f, % .2f) g, (% 3.2f, % 3.2f, % 3.2f) º/s, % 2.2f ºC", n + 1, data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
	}
	if (maxA > 1.4) {
		status = C_RUNNING;
	}
	else if (minA < 0.5) {
		status = C_FALLING;
	}
	else if (minA < 0.9 || maxA > 1.1) {
		status = C_WALKING;
	}
	else if (maxZ > 0.9) {
		status = C_LYING;
	}
	else {
		status = C_STOPPED;
	}
	if (maxG > 300) {
		if (minA < 0.5 && maxA < 1.1) {
			status = C_FALLING;
		}
		else {
			status = C_TROUBLED;
		}
	}
	if (maxA == 0 && minA == 0) {
		status = C_INACTIVE;
	}
	//printf("ACC: [% .2f, % .2f], GIR: [% .2f, % .2f]\n", minA, maxA, minG, maxG);
	return status;
}

int listAreas(char (*areaList)[16], char (*area)[256]) {
	int max = 0;
	char areaLine[1024], *token;
	FILE *fp;

	fp = fopen (F_SETUP, "r");
	for (int i = 0; fgets(areaLine, MAX, fp); i ++) {
		if (areaLine[strlen(areaLine) - 1] == '\n') {
			areaLine[strlen(areaLine) - 1] = '\0';
		}

		token = strtok(areaLine, " ");
		strcpy(areaList[i], token);
		while (token != NULL) {
			token = strtok(NULL, " ");
			if (token != NULL) {
				strcpy(area[atoi(token)], areaList[i]);
				if (atoi(token) > max) {
					max = atoi(token);
				}
			}
		}
	}
	printf("> Lista de sujeitos (@Área, #ISu):\n");
	for (int i = 0; strlen(areaList[i]) > 0; i ++) {
		printf("* @%s", areaList[i]);
		for (int j = 1; j <= max; j ++) {
			if (strlen(area[j]) > 0 && strcasecmp(area[j], areaList[i]) == 0) {
				printf(" #%d", j);
			}
		}
		printf("\n");
		
	}
	fclose(fp);
	return max;
}

void getAreas(char (*areaList)[16], char (*area)[256]) {
	int max = 0;
	char areaLine[1024], adString[32], *token;
	struct in_addr adIp;
	FILE *fp;

	fp = fopen ("areas.cfg", "r");
	for (int i = 0; fgets(areaLine, MAX, fp); i ++) {
		if (areaLine[strlen(areaLine) - 1] == '\n') {
			areaLine[strlen(areaLine) - 1] = '\0';
		}

		token = strtok(areaLine, " ");
		strcpy(areaList[i], token);
		while (token != NULL) {
			token = strtok(NULL, " ");
			if (token != NULL) {
				strcpy(area[atoi(token)], areaList[i]);
				if (atoi(token) > max) {
					max = atoi(token);
				}
			}
		}
	}
	fclose(fp);
}

int setArea(char (*areaList)[16], char (*area)[256], int isu, char tempArea[], int max) {
	int found = 0;
	FILE *fp;
	
	fp = fopen ("areas.cfg", "w");
	fclose(fp);
	fp = fopen ("areas.cfg", "a");

	if (isu > max) {
		max = isu;
	}

	strcpy(area[isu], tempArea);
	for (int i = 0; strlen(areaList[i]) > 0; i ++) {
		fprintf(fp, "%s", areaList[i]);
		for (int j = 1; j <= max; j ++) {
			if (strlen(area[j]) > 0 && strcasecmp(area[j], areaList[i]) == 0) {
				if (j == isu) {
					found = isu;
				}
				strcpy(area[j], areaList[i]);
				fprintf(fp, " %d", j);
			}
		}
		fprintf(fp, "\n");
	}
	if (found == 0) {
		memset(area[isu], 0, sizeof(area[isu]));
	}
	//printf("area[%d] = %s\n", isu, area[isu]);
	fclose(fp);
	return found;
}

void userOut(int messageType, char status[], int show, char area[]) {
	int a = 0;
	time_t tsLong = 0;
	char tsString[32], tsiString[32], tsfString[32], isuString[64], pmString[32], paString[32], nsString[32], areaString[32];
	struct in_addr adIp;

	//adIp.s_addr = message.ad;
	//printf("%x\n", message.ad);
	
	/*strftime(tsString, 32, "%Y-%m-%d %X", localtime(&message.ts));
	strftime(tsiString, 32, "%Y-%m-%d %X", localtime(&message.tsi));
	strftime(tsfString, 32, "%Y-%m-%d %X", localtime(&message.tsf));*/


	switch (messageType) {
		case T_HUBDIS:
			adIp.s_addr = hubdis.ad;
			printf("> %s | Desconexão de concentrador | TCP/IP: %s:%d;\n", tStamp(time(NULL)), inet_ntoa(adIp), hubdis.port);
			break;
		case T_HUBCON:
			adIp.s_addr = hubcon.ad;
			printf("> %s | Conexão aceite com concentrador | TCP/IP: %s:%d;\n", tStamp(time(NULL)), inet_ntoa(adIp), hubcon.port);
			break;
		case T_ERROR:
			if (error.isu == 0) {
				strcpy(isuString, "<SERVICE>");
			}
			else {
				adIp.s_addr = error.ad;
				if (strlen(area) > 0) {
					sprintf(isuString, "#%d @%s | IP: %s", error.isu, area, inet_ntoa(adIp));
				}
				else {
					sprintf(isuString, "#%d <NO AREA> | IP: %s", error.isu, inet_ntoa(adIp));
				}
			}
			printf("> %s | ERROR (%d) %s - %s;\n", tStamp(error.ts), error.er, isuString, erString[error.er]);
			break;
		case T_CONN:
			adIp.s_addr = conn.ad;
			if (strlen(area) > 0) {
				sprintf(areaString, "@%s", area);
			}
			else {
				strcpy(areaString, "<NO AREA>");
			}
			printf("> %s | Conexão estabelecida com #%d %s | IP: %s;\n", tStamp(conn.ts), conn.isu, areaString, inet_ntoa(adIp));
			break;
		case T_START:
			adIp.s_addr = start.ad;
			if (start.isu == 0) {
				strcpy(isuString, "<ALL>");
			}
			else {
				if (strlen(area) > 0) {
					sprintf(isuString, "#%d @%s | IP: %s", start.isu, area, inet_ntoa(adIp));
				}
				else {
					sprintf(isuString, "#%d <NO AREA> | IP: %s", start.isu, inet_ntoa(adIp));
				}
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
			printf("> %s | START %s | PM = %s | PA = %s | NS = %s;\n", tStamp(start.ts), isuString, pmString, paString, nsString);
			break;
		case T_DATA:
			adIp.s_addr = data.ad;
			if (strlen(area) > 0) {
				sprintf(areaString, "@%s", area);
			}
			else {
				strcpy(areaString, "<NO AREA>");
			}
			if (start.ns == 1) {
				sprintf(nsString, "%d amostra", data.ns);
			}
			else {
				sprintf(nsString, "%d amostras", data.ns);
			}
			if (data.isu > 0) {
				printf("> %s | %s #%d %s | IP: %s | PM = %d ms | PA = %d μs | NS = %s", tStamp(data.ts), status, data.isu, areaString, inet_ntoa(adIp), data.pm, data.pa, nsString);
				if (show > 0) {
					printf(":");
					for (int n = 0; n < data.ns; n ++) {
						printf("\n* S%d: (% .2f, % .2f, % .2f) g, (% 3.2f, % 3.2f, % 3.2f) º/s, % 2.2f ºC", n + 1, data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
						if (n < data.ns - 1) {
							printf(",");
						}
					}
				}
				printf(";\n");
			}
			break;
		case T_STOP:
			adIp.s_addr = stop.ad;			
			if (stop.isu == 0) {
				strcpy(isuString, "<ALL>");
			}
			else {
				if (strlen(area) > 0) {
					sprintf(isuString, "#%d @%s | IP: %s", stop.isu, area, inet_ntoa(adIp));
				}
				else {
					sprintf(isuString, "#%d <NO AREA> | IP: %s", stop.isu, inet_ntoa(adIp));
				}
			}
			printf("> %s | STOP %s | Razão de paragem: %s;\n", tStamp(stop.ts), isuString, stop.sr);
			break;
		default:
			break;
	}
}

void writeDB (int messageType, char status[], char area[]) {
	FILE *fp;
	time_t tsLong = 0;
	char tsString[32], isuString[32], pmString[32], paString[32], nsString[32];
	struct in_addr adIp;

	//adIp.s_addr = message.ad;
	//printf("%x\n", message.ad);
	
	/*strftime(tsString, 32, "%Y-%m-%d %X", localtime(&message.ts));
	strftime(tsiString, 32, "%Y-%m-%d %X", localtime(&message.tsi));
	strftime(tsfString, 32, "%Y-%m-%d %X", localtime(&message.tsf));*/

	fp = fopen(DB_MESSAGE, "a");
	switch (messageType) {
		case T_HUBDIS:
			adIp.s_addr = hubdis.ad;
			fprintf(fp, "%s DISCONNECTION HUB %s:%d\n", tStamp(time(NULL)), inet_ntoa(adIp), hubdis.port);
			break;
		case T_HUBCON:
			adIp.s_addr = hubcon.ad;
			fprintf(fp, "%s CONNECTION HUB %s:%d\n", tStamp(time(NULL)), inet_ntoa(adIp), hubcon.port);
			break;
		case T_ERROR:			
			if (error.isu > 0) {
				adIp.s_addr = error.ad;
				sprintf(isuString, "%d %s", error.isu, inet_ntoa(adIp));
			}
			else {
				strcpy(isuString, "SERVICE");
			}
			fprintf(fp, "%s ERROR (%d) %s\n", tStamp(error.ts), error.er, isuString);
			break;
		case T_CONN:
			adIp.s_addr = conn.ad;
			fprintf(fp, "%s CONNECTION %d %s\n", tStamp(conn.ts), conn.isu, inet_ntoa(adIp));
			break;
		case T_START:
			adIp.s_addr = start.ad;
			if (start.isu > 0) {
				sprintf(isuString, "%d %s", start.isu, inet_ntoa(adIp));
			}
			else {
				strcpy(isuString, "ALL");
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
			fprintf(fp, "%s START %s %s %s %s\n", tStamp(start.ts), isuString, pmString, paString, nsString);
			break;
		case T_DATA:
			adIp.s_addr = data.ad;
			if (data.isu > 0) {
				fprintf(fp, "%s DATA %d %s %d %d %d | ", tStamp(data.ts), data.isu, inet_ntoa(adIp), data.pm, data.pa, data.ns);
				for (int n = 0; n < data.ns; n ++) {
					fprintf(fp, "%.2f %.2f %.2f %.2f %.2f %.2f %.2f", data.sax[n], data.say[n], data.saz[n], data.sgx[n], data.sgy[n], data.sgz[n], data.st[n]);
					if (n < data.ns - 1) {
						fprintf(fp, " | ");
					}
				}
				fprintf(fp, "\n");
			}
			fclose(fp);
			fp = fopen(DB_CONDITION, "a");
			fprintf(fp, "%s %s %d %s\n", tsString, status, data.isu, area);
			break;
		case T_STOP:
			if (stop.isu > 0) {
				sprintf(isuString, "%d %s", stop.isu, inet_ntoa(adIp));
			}
			else {
				strcpy(isuString, "ALL");
			}
			fprintf(fp, "%s STOP %s %s\n", tStamp(stop.ts), isuString, stop.sr);
			break;
		default:
			break;
	}
	fclose(fp);
}

void sendWeb (int flag, int isu, int messageType, char status[], char area[]) {
	char request[MAX], tsString[32];
	time_t tsLong;
	if (flag == 's') {
		sprintf(request, "curl \"%sapi.php/?Q=2&ise=%d&name='%s'\"", WEB, ISE, NAME);
		//write(1, request, strlen(request));
		//write(1, "\n", 1);
		system(request);
	}
	else if (flag == 'u') {
		sprintf(request, "curl \"%sapi.php/?Q=3&ise=%d&isu=%d&area='%s'\"", WEB, ISE, isu, area);
		//write(1, request, strlen(request));
		//write(1, "\n", 1);
		if (fork() == 0) execl("/bin/sh", "sh", "-c", request, NULL);
	}
	else {
		switch (messageType) {
			/*case T_START:
				tsLong = start.ts;
				strftime(tsString, 32, "%Y-%m-%d|%X", localtime(&tsLong));
				sprintf(request, "curl \"http://your-server-name/api.php/?Q=4&id=%d&ise=%d&isu=%d&tp='ONLINE'&ts='%s'\"", i, ISE, start.isu, tsString);
				write(1, request, strlen(request));
				write(1, "\n", 1);
				if (fork() == 0) execl("/bin/sh", "sh", "-c", request, NULL);
				break;*/
			case T_DATA:
				tsLong = data.ts;
				strftime(tsString, 32, "%Y-%m-%d|%X", localtime(&tsLong));
				sprintf(request, "curl \"%sapi.php/?Q=4&ise=%d&isu=%d&tp='%s'&ts='%s'\"", WEB, ISE, data.isu, status, tsString);
				//write(1, request, strlen(request));
				//write(1, "\n", 1);
				if (fork() == 0) execl("/bin/sh", "sh", "-c", request, NULL);
				break;
			/*case T_STOP || T_ERROR:
				tsLong = stop.ts;
				strftime(tsString, 32, "%Y-%m-%d|%X", localtime(&tsLong));
				sprintf(request, "curl \"http://your-server-name/api.php/?Q=4&id=%d&ise=%d&isu=%d&tp='OFFLINE'&ts='%s'\"", i, ISE, stop.isu, tsString);
				write(1, request, strlen(request));
				write(1, "\n", 1);
				if (fork() == 0) execl("/bin/sh", "sh", "-c", request, NULL);
				break;*/
			default:
				break;
		}
	}
}

void readDB (int isu, char tempArea[], time_t tsi, time_t tsf) {
	int tempIsu = 0, count = 0;
	char areaLine[256], tsString[32], tsiString[32], tsfString[32], status1[32], status2[32], tempArea2[32], areaString[32];
	struct tm info;
	time_t tempTs = 0;
	FILE *fp;

	fp = fopen (DB_CONDITION, "r");
	if (isu == 0 && strlen(tempArea) == 0) {
		printf("> Comportamentos de %s até %s:\n", tStamp(tsi), tStamp(tsf));
	}
	else if (isu == 0) {
		printf("> Comportamentos em @%s de %s até %s:\n", tempArea, tStamp(tsi), tStamp(tsf));
	}
	else {
		printf("> Comportamentos de #%d de %s até %s:\n", isu, tStamp(tsi), tStamp(tsf));
	}
	if (fp != NULL) {
		for (int i = 0; fgets(areaLine, MAX, fp) && tempTs <= tsf; i ++) {
			if (areaLine[strlen(areaLine) - 1] == '\n') {
				areaLine[strlen(areaLine) - 1] = '\0';
			}
			memset(tempArea2, 0, sizeof(tempArea2));
			sscanf(areaLine, "%d-%d-%d %d:%d:%d %s %s %d %s", &info.tm_year, &info.tm_mon, &info.tm_mday, &info.tm_hour, &info.tm_min, &info.tm_sec, status1, status2, &tempIsu, tempArea2);
			
			info.tm_year -= 1900;
			info.tm_mon --;
			tempTs = mktime(&info);
			if (tempTs >= tsi && tempTs <= tsf) {
				if (isu == 0 && strlen(tempArea) == 0) {
					if (strlen(tempArea2) > 0) {
						sprintf(areaString, "@%s", tempArea2);
					}
					else {
						strcpy(areaString, "<NO AREA>");
					}
					printf("* %s | %s %s #%d %s,\n", tStamp(tempTs), status1, status2, tempIsu, areaString);
					count ++;
				}
				else if (isu == 0) {
					if (!strcasecmp(tempArea2, tempArea)) {
						printf("* %s | %s %s #%d @%s,\n", tStamp(tempTs), status1, status2, tempIsu, tempArea2);
						count ++;
					}
				}
				else {
					if (tempIsu == isu) {
						if (strlen(tempArea2) > 0) {
							sprintf(areaString, "@%s", tempArea2);
						}
						else {
							strcpy(areaString, "<NO AREA>");
						}
						printf("* %s | %s %s #%d %s,\n", tStamp(tempTs), status1, status2, tempIsu, areaString);
						count ++;
					}
				}
			}
		}
		fclose(fp);
	}
	printf("* %d resultados;\n", count);
}

void getUsers(unsigned char isuList[]) {
	int i = 0;
	char areaLine[1024], *token;
	struct in_addr adIp;
	FILE *fp;
	memset(isuList, 0, 256);

	fp = fopen ("areas.cfg", "r");
	while (fgets(areaLine, MAX, fp)) {
		if (areaLine[strlen(areaLine) - 1] == '\n') {
			areaLine[strlen(areaLine) - 1] = '\0';
		}
		token = strtok(areaLine, " ");
		while (token != NULL) {
			token = strtok(NULL, " ");
			if (token != NULL) {
				isuList[i] = atoi(token);
				i ++;
			}
		}
	}
	fclose(fp);
}

void receiveHub (int hubSock, int show) {
	int messageType;
	size_t size;
	char *status, buffer[MAX], areaList[16][16], area[16][256], isuArea[256];
	unsigned char isuList[256];
	
	printf("in receiving\n");
	status = malloc(MAX_S);

	memset(buffer, 0, sizeof(buffer));
	while ((size = recv(hubSock, buffer, sizeof(buffer), 0)) > 0) {
		printf("in while with %d %d %d %d %d %d from %ld\n", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], size);
		messageType = buffer[0];
		memset(area, 0, sizeof(area));
		getAreas(areaList, area);

		switch (messageType) {
			case T_ERROR:
				//printf("error\n");
				memcpy(&error, buffer, sizeof error);
				strcpy(isuArea, area[error.isu]);
				break;
			case T_CONN:
				//printf("conn\n");
				memcpy(&conn, buffer, sizeof conn);
				strcpy(isuArea, area[conn.isu]);
				break;
			case T_START:
				//printf("start\n");
				printf("got a start\n");
				memcpy(&start, buffer, sizeof start);
				strcpy(isuArea, area[start.isu]);
				break;
			case T_DATA:
				//printf("data\n");
				memcpy(&tempData, buffer, sizeof tempData);
				//printf("receiving: %d %d %d %d %d %d %d", tempData.tp, tempData.ts, tempData.ad, tempData.isu, tempData.pm, tempData.pa, tempData.ns);
				data.tp = tempData.tp;
				data.ts = tempData.ts;
				data.ad = tempData.ad;
				data.isu = tempData.isu;
				data.pm = tempData.pm;
				data.pa = tempData.pa;
				data.ns = tempData.ns;
				for (int n = 0; n < data.ns; n ++) {
			//    printf("|%d %d %d %d %d %d %d", tempData.sax[n], tempData.say[n], tempData.saz[n], tempData.sgx[n], tempData.sgy[n], tempData.sgz[n], tempData.st[n]);
					data.sax[n] = (float) tempData.sax[n] / 100;
					data.say[n] = (float) tempData.say[n] / 100;
					data.saz[n] = (float) tempData.saz[n] / 100;
					data.sgx[n] = (float) tempData.sgx[n] / 100;
					data.sgy[n] = (float) tempData.sgy[n] / 100;
					data.sgz[n] = (float) tempData.sgz[n] / 100;
					data.st[n] = (float) tempData.st[n] / 100;
				}
				strcpy(isuArea, area[data.isu]);
						//printf("\n");
				status = getStatus();
				sendWeb('m', 0, messageType, status, isuArea);
				break;
			case T_STOP:
				//printf("stop\n");
				memcpy(&stop, buffer, sizeof stop);
				strcpy(isuArea, area[stop.isu]);
				break;
			case T_USER:
				printf("getting users");
				getUsers(isuList);
				//printf("sending %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", isuList[0], isuList[1], isuList[2], isuList[3], isuList[4], isuList[5], isuList[6], isuList[7], isuList[9], isuList[8], isuList[10], isuList[11], isuList[12], isuList[13], isuList[14], isuList[15]);
				send(hubSock, isuList, strlen(isuList), 0);
			default:
				break;
		}
		if (show >= 0) {
			userOut(messageType, status, show, isuArea);
		}
		writeDB(messageType, status, isuArea);
	}
	printf("after receiving\n");
}

void printMenu() {
	printf("> Menu de comandos disponíveis:\n");
	printf("* l/list - mostrar lista de sujeitos monitorizados e áreas disponíveis,\n");
	printf("* a/area <área> <ISu[N]> - atribuir área a N sujeitos (área inexistente: remover N sujeitos das respetivas áreas),\n");
	printf("* d/database <ISu/área> <TSI (Y-M-D H:M:S)> <TSF (Y-M-D H:M:S)> - mostrar comportamentos de um sujeito ou numa área num intervalo de tempo,\n");
	printf("* s/show <modo> - mostrar mensagens recebidas em tempo real (modo = 0: não mostrar valores dos sensores, modo > 0: mostrar valores),\n");
	printf("* h/hide - ocultar mensagens recebidas,\n");
	printf("* m/menu - mostrar o menu;\n");
}

int main() { 
	int serviceSock, hubSock, valread,clientaddress, messageType, max = 0, tempIsu = 0, isu = 0, *show = 0, aborted = 0, id = -1;
	int bound, listened;
	char tsString[1024], tempArea[16], out[MAX], *status, *token;
	char areaList[16][16], area[16][256], isuArea[256];
	struct sockaddr_in serviceAddr, hubAddr;
	socklen_t addrSize;
	struct sockaddr_in* pV4Addr;
	struct in_addr ipAddr;
  	struct tm info;
	char str[INET_ADDRSTRLEN];
	int opt = 1; 
	uint16_t clientPort;
	//int addrlen = sizeof(address); 
	char buffer[2048], input[1024]; 
	int j=0,i=0;
	time_t tsi = 0, tsf = 0;
	ssize_t readSize = 0;
	
	status = malloc(32);

	printf("> A inicializar Gestor de Serviços...\n");
	// Creating socket file descriptor 

	serviceSock = socket(AF_INET, SOCK_STREAM, 0);
	
	if (serviceSock <= 0) {
		error.ts = time(NULL);
		error.ad = 0;
		error.isu = 0;
		error.er = E_OPENSOCKET;
		userOut(T_ERROR, NULL, *show, NULL);
		writeDB(T_ERROR, NULL, NULL);
		perror("");
		exit(1);
	}
	printf("> %s | Socket de servidor aberto.\n", tStamp(time(NULL)));
	
	memset(&serviceAddr, 0, sizeof(serviceAddr));
	serviceAddr.sin_family = AF_INET;
	serviceAddr.sin_addr.s_addr = INADDR_ANY;
	serviceAddr.sin_port = htons(PORT);
	
	// Forcefully attaching socket to the port 8080 
	bound = bind(serviceSock, (struct sockaddr*)&serviceAddr, sizeof(serviceAddr));
	if (bound < 0) {
		error.ts = time(NULL);
		error.ad = 0;
		error.isu = 0;
		error.er = E_BINDING;
		userOut(T_ERROR, NULL, *show, NULL);
		writeDB(T_ERROR, NULL, NULL);
		perror("");
		exit(1);
	}
	printf("> %s | Binding feito na porta %d.\n", tStamp(time(NULL)), PORT);
    
    listened = listen(serviceSock, MAX_CLIENTS);
	if (listened < 0) {		
		error.ts = time(NULL);
		error.ad = 0;
		error.isu = 0;
		error.er = E_LISTENING;
		userOut(T_ERROR, NULL, *show, NULL);
		writeDB(T_ERROR, NULL, NULL);
		perror("");
		exit(1);
	}
	printf("> A conectar com o Sistema Central | URL: %s...\n", WEB);
	sendWeb('s', 0, 0, NULL, NULL);
	printf("> %s | Gestor de Serviços de %s #%d inicializado.\n", tStamp(time(NULL)), NAME, ISE);	
	show = mmap (NULL, sizeof *show, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
	max = listAreas(areaList, area);
	for (int i = 1; i <= max; i ++) {
		if (strlen(area[i]) > 0) {
			sendWeb('u', i, 0, NULL, area[i]);
		}
	}
	printMenu();
	printf("> À procura de conexões de concentradores...\n");
	if (fork() == 0) {
		while (1) {
			hubSock = accept(serviceSock, (struct sockaddr*)&hubAddr, &addrSize);
			if (hubSock < 0) {
				error.ts = time(NULL);
				error.ad = 0;
				error.isu = 0;
				error.er = E_ACCEPTING;
				userOut(T_ERROR, NULL, *show, NULL);
				writeDB(T_ERROR, NULL, NULL);
				perror("");
				exit(1);
			}
			hubcon.ad = (long int) inet_addr(inet_ntoa(hubAddr.sin_addr));
			hubcon.port = ntohs(hubAddr.sin_port);
			userOut(T_HUBCON, NULL, *show, NULL);
			writeDB(T_HUBCON, NULL, NULL);
			//printf("> Conexão aceite com concentrador | TCP/IP: %s:%d.\n", inet_ntoa(hubAddr.sin_addr), ntohs(hubAddr.sin_port));			
			if (fork() == 0) {
				close(serviceSock);
				receiveHub(hubSock, *show);
				
				hubdis.ad = (long int) inet_addr(inet_ntoa(hubAddr.sin_addr));
				hubdis.port = ntohs(hubAddr.sin_port);
				userOut(T_HUBDIS, NULL, *show, NULL);
				writeDB(T_HUBDIS, NULL, NULL);
				//printf("> %s | Desconexão de concentrador | TCP/IP: %s:%d.\n", inet_ntoa(newAddr.sin_addr), ntohs(newAddr.sin_port));
				exit(0);
			}			
		}
	}
	else {
		while (1) {
			//printf("<Area> <ISu[]>: ");
			fgets(input, MAX, stdin);
			input[strlen(input) - 1] = '\0';
			if ((token = strtok(input, " ")) != NULL) {
				if (!strcasecmp(token, "l") || !strcasecmp(token, "list")) {
					max = listAreas(areaList, area);
					*show = -1;
					printf("> Escreva \"sh\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
				}
				else if (!strcasecmp(token, "a") || !strcasecmp(token, "area")) {
					if ((token = strtok(NULL, " ")) != NULL) {
						strcpy(tempArea, token);
						while (token != NULL) {
							token = strtok(NULL, " ");
							if (token != NULL) {														
								isu = setArea(areaList, area, atoi(token), tempArea, max);
								sendWeb('u', atoi(token), 0, NULL, area[isu]);
							}
						}
						max = listAreas(areaList, area);
					}
					else {
						printf("> entrada inválida...\n");
					}
					*show = -1;
					printf("> Escreva \"sh\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
				}
				else if (!strcasecmp(token, "d") || !strcasecmp(token, "db") || !strcasecmp(token, "database")) {
					if ((token = strtok(NULL, " ")) != NULL) {
						if (strchr(token, '-') == NULL) {
							tempIsu = atoi(token);
							if (tempIsu == 0) {
								strcpy(tempArea, token);
							}
							else {
								memset(tempArea, 0, sizeof(tempArea));
							}
							token = strtok(NULL, " ");
						}
						else {
							tempIsu = 0;
							memset(tempArea, 0, sizeof(tempArea));
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
					}
					else {
						tempIsu = 0;
						memset(tempArea, 0, sizeof(tempArea));
						tsi = 0;
						tsf = time(NULL);
					}
					readDB(tempIsu, tempArea, tsi, tsf);
					*show = -1;
					printf("> Escreva \"s\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
				}
				else if (!strcasecmp(token, "s") || !strcasecmp(token, "show")) {
					if ((token = strtok(NULL, " ")) != NULL) {
						*show = atoi(token);
					}
					else {
						*show = 0;
					}
				}
				else if (!strcasecmp(token, "h") || !strcasecmp(token, "hide")) {
					*show = -1;
					printf("> Escreva \"s\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
				}
				else if (!strcasecmp(token, "m") || !strcasecmp(token, "menu")) {				
					printMenu();
					*show = -1;
					printf("> Escreva \"s\"/\"show\" para mostrar as mensagens recebidas em tempo real;\n");
				}
				else {
					printf("> entrada inválida...\n");
				}
			}
			//sscanf(input, "%d %s", &isu, tempArea);
		}
	}
	return 0; 
} 
