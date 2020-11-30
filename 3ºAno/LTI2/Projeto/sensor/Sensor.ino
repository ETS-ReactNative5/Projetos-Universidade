#include <I2Cdev.h>
#include <MPU6050.h>
#include <Wire.h>
#include <WiFi.h>
#include <math.h>
#include <WiFiUdp.h>

#define MAX 2048
#define MAX_A 64
#define T_ERROR -2
#define T_COMS 1
#define T_ACCEPT 2
#define T_START 3
#define T_DATA 4
#define T_STOP 5
#define PA  1000     //microseconds
#define PM  1000      //milliseconds
#define NS  5         //amostras
#define ISS 1

MPU6050 mpu;

int16_t accelX, accelY, accelZ;
float gForceX, gForceY, gForceZ;
long tsEnd,tsBegin;
int16_t gyroX, gyroY, gyroZ;
int16_t temp;
float rotX, rotY, rotZ;
unsigned long int ip3,ip2,ip1,ip0;

typedef struct message {
  byte tp;         //0=setup, -1=seterr, 1=start, 2=data, -2=error, 3=stop
  unsigned long ts;     //setup, seterr, start, data, error, stop, exit  ->tempo da primeira amostra
  byte iss;         //data
  unsigned int ad;          
  int pm;          //start, data
  int pa;          //start, data
  byte ns;          //start, data
  byte er;         //error
  char sr[32];     //stop
  char com[32];    //setup
  int16_t sax[MAX_A];  //data
  int16_t say[MAX_A];  //data
  int16_t saz[MAX_A];  //data
  int16_t sgx[MAX_A];  //data
  int16_t sgy[MAX_A];  //data
  int16_t sgz[MAX_A];  //data
  int16_t st[MAX_A];
  
} Message;

Message message;

WiFiUDP udp;
const char* ssid = "andrealmeida";
const char* pwd =  "12345678";
const char * udpAddress = "192.168.137.227";
const int udpPort = 12900;

char buffer[1024];
char dataS[2048];

int n = 0;
bool start = false;
bool firstTime = true;
bool flag = true;
int MessageSize = 0, dataSize=0;

void handShake(){
    udp.beginPacket(udpAddress, udpPort);
    udp.write((const uint8_t*)dataS,6);
    udp.endPacket();
}

void createStringToSend(int type){
  memset(dataS,0, 2048);
  //Serial.print(String("")+"Data: "+dataS);
  char buf[100];

  if (type == T_DATA){                             //enviar string DATA
    //dataS = String("")+message.tp+" "+message.ts+" "+message.iss+" "+message.pm+" "+message.pa+" "+message.ns;
    dataS[0] = message.tp;
    
    dataS[1] = message.ts >> 24;
    dataS[2] = message.ts >> 16;
    dataS[3] = message.ts >> 8;
    dataS[4] = message.ts;
    
    dataS[5] = message.iss;

    dataS[6] = message.pm >> 24;
    dataS[7] = message.pm >> 16;
    dataS[8] = message.pm >> 8;
    dataS[9] = message.pm;

    dataS[10] = message.pa >> 24;
    dataS[11] = message.pa >> 16;
    dataS[12] = message.pa >> 8;
    dataS[13] = message.pa;

    dataS[14] = message.ns;

    for(int n =0;n<message.ns;n++){
      memset(buf,0,100);
      //sprintf(buf, "|%d %d %d %d %d %d %d", message.sax[n],message.say[n],message.saz[n],message.sgx[n],message.sgy[n],message.sgz[n],message.st[n]);
      dataS[15+14*n] = message.sax[n] >> 8;
      dataS[16+14*n] = message.sax[n];
      dataS[17+14*n] = message.say[n] >> 8;
      dataS[18+14*n] = message.say[n];
      dataS[19+14*n] = message.saz[n] >> 8;
      dataS[20+14*n] = message.saz[n];
      dataS[21+14*n] = message.sgx[n] >> 8;
      dataS[22+14*n] = message.sgx[n];
      dataS[23+14*n] = message.sgy[n] >> 8;
      dataS[24+14*n] = message.sgy[n];
      dataS[25+14*n] = message.sgz[n] >> 8;
      dataS[26+14*n] = message.sgz[n];
      dataS[27+14*n] = message.st[n] >> 8;
      dataS[28+14*n] = message.st[n];
      //String bufData= String (buf);
      //strcat(dataS,bufData);
      //dataS.concat(bufData);
      MessageSize = n;
    }
    dataSize=29+14*MessageSize;
    
  }else{                                    //enviar string ERROR
    message.er = 5;
    dataS[0] = message.tp;
    
    dataS[1] = message.ts >> 24;
    dataS[2] = message.ts >> 16;
    dataS[3] = message.ts >> 8;
    dataS[4] = message.ts;

    dataS[5] = ip0;
    dataS[6] = ip1;
    dataS[7] = ip2;
    dataS[8] = ip3;

    dataS[9] = message.er;

    dataSize = 10;
    //dataS = String("")+message.tp+" "+message.ts+" "+message.ad+" "+message.er;
  }
}

void sendDatatoServer(int type){  
  createStringToSend( type );
  Serial.print(String("")+"Data: ");
  for (int i = 0; i < dataSize; i ++) {
    printf("%d ",dataS[i]);
  }
  printf("\n");
  udp.beginPacket(udpAddress, udpPort);
  udp.write((const uint8_t*)dataS,dataSize);
  udp.endPacket();
  
}
void receiveDataFromServer(){
  char receivedData[1024];
  char tempstr[1024];
  char* token; 
  memset(receivedData,0,1024);
  
  int n = udp.parsePacket();

  if(udp.read(receivedData, 1024) > 0){
    
    strcpy(tempstr, receivedData);
    firstTime = false;
    token = strtok(receivedData, " ");
    //message.tp = atoi(token);
    message.tp = receivedData[0];
    Serial.println(String("")+"Type: "+message.tp);
    if(message.tp == T_START){
      Serial.println("start");
      //sscanf(tempstr,"%d %ld %d %d %d",&message.tp, &message.ts, &message.pm, &message.pa, &message.ns);
      message.ts = receivedData[1]* pow(2, 24)+ receivedData[2]* pow(2, 16)+receivedData[3]* pow(2, 8)+receivedData[4];
      message.pm = receivedData[5]* pow(2, 24)+ receivedData[6]* pow(2, 16)+receivedData[7]* pow(2, 8)+receivedData[8];
      message.pa = receivedData[9]* pow(2, 24)+ receivedData[10]* pow(2, 16)+receivedData[11]* pow(2, 8)+receivedData[12];
      message.ns = receivedData[13];
      Serial.print(String("")+"Parametros: "+message.pm+" "+message.pa+" "+message.ns);
      if(message.pm <= 0)   message.pm= PM;
      
      if(message.pa <= 0)   message.pa = PA;
      
      if(message.ns <= 0)   message.ns = NS;
      
      start = true;
    }
    if(message.tp == T_STOP){
      //sscanf(receivedData,"%d %ld %s",&message.tp, &message.ts, message.sr);
      message.ts = receivedData[1]* pow(2, 24)+ receivedData[2]* pow(2, 16)+receivedData[3]* pow(2, 8)+receivedData[4];
      strcpy(message.sr,&receivedData[5]);
      start = false;
    }
  }
}

void setup() {
  Serial.begin(115200);
  Wire.begin();

  //Connect to the WiFi network
  WiFi.begin(ssid, pwd);
  Serial.println("");

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(String(WiFi.localIP()));
  
  //This initializes udp and transfer buffer
  udp.begin(udpPort);
  
  message.tp = T_COMS;
  message.iss = ISS;
  sscanf(WiFi.localIP().toString().c_str(), "%d.%d.%d.%d", &ip3, &ip2, &ip1, &ip0);
  message.ad = ip0*(256*256*256) + ip1*(256*256) + ip2*(256) + ip3;
  Serial.print(String("")+"Message: "+message.tp+" "+message.iss+" "+message.ad);
  dataS[0] = message.tp;
  dataS[1] = ip0;
  dataS[2] = ip1;
  dataS[3] = ip2;
  dataS[4] = ip3;
  dataS[5] = message.iss;
  Serial.print(String("")+"Data: ");
  for (int i = 0; i < 6; i ++) {
    printf("%x ", dataS[i]);
  }
  printf("\n");
  setupMPU();
  Serial.println(mpu.getSleepEnabled()? "Awake" : "Asleep");
}


void loop() {

  if(firstTime){
    handShake();
    delay(500);
  }
  
  receiveDataFromServer();      //verifica se recebeu dados do server
  
  if(start){
    if(n < message.ns){
      if (n == 0){
        tsBegin=micros();
        message.tp = T_DATA;
        message.iss = ISS;
      }
      recordRegisters();
      //verificar se ultrapassou o tempo de mensagem
      long timeElapsed = millis()-(tsBegin/1000);
      Serial.println(String("")+"TimeElapsed: "+timeElapsed);
      if(n < message.ns && timeElapsed > message.pm){           //se ultrapassado enviar erro e reset variaveis
        message.tp = T_ERROR;
        message.ts = getTime();
        int type = T_ERROR;
        Serial.println("Erro");
        sendDatatoServer(type);
        n = 0;
        start = false;
      }
      
    }else{
      n = 0;
      tsEnd= micros();
      long unix = getTime(); 
      message.ts= unix;       //atualiza timestamp
      Serial.println(String("")+"pm: "+message.pm+" ns: "+message.ns+" pa: "+message.pa);
      if( message.pm != (message.pa/1000)*message.ns ){
        Serial.println(String("")+message.pm+" end: "+tsEnd+" : "+tsBegin);
        delayMicroseconds(message.pm*1000-tsEnd+tsBegin);
      }
      Serial.println(String("")+"PM: "+message.pm+" Delay: "+(message.pm*1000-tsEnd+tsBegin)+" Time Elapsed: "+(tsEnd-tsBegin));
      int type = T_DATA;
      sendDatatoServer(type);
      Serial.println("\nDone");
    }
  }
  //delay(100);
}

void setupMPU(){

  mpu.initialize();
  mpu.setFullScaleAccelRange(0);
  mpu.setFullScaleGyroRange ( MPU6050_GYRO_FS_250 );
  
  startCalibration();
  
  //offsets
  Serial.println(String("")+"Xoff: "+mpu.getXAccelOffset()+" Yoff: "+mpu.getYAccelOffset()+" Zoff: "+mpu.getZAccelOffset()+" GXoff: "+mpu.getXGyroOffset()+" GYoff: "+mpu.getYGyroOffset()+" GZoff: "+mpu.getZGyroOffset()); 
}

void recordRegisters() {
  
  mpu.getMotion6(&accelX, &accelY, &accelZ, &gyroX, &gyroY, &gyroZ );
  temp = mpu.getTemperature();
  message.sax[n]=accelX;
  message.say[n]=accelY;
  message.saz[n]=accelZ;
  message.sgx[n]=gyroX;
  message.sgy[n]=gyroY;
  message.sgz[n]=gyroZ;
  message.st[n]=temp;
  n++;
  delayMicroseconds(message.pa);      //delay para cada amostra
}
