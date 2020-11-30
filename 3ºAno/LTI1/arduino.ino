#include <FastCRC.h>
#include <FastCRC_cpu.h>
#include <FastCRC_tables.h>
#include <SPI.h>
#include <nRF24L01.h>
#include <RF24.h>
#include <time.h>

RF24 radio(7, 8);
FastCRC8 CRC8;

typedef struct pack {
  byte typeSnLength = 0x80 * 0 + 0x20 * 0 + 0; // byte 0 | Type: bit 7 (MSB) (>0x80), SN: bits 5-6 (>0x20), payload length: bits 0(LSB)-5 (>0x00)
  byte adresses     = 0x10 * 0 + 0;            // byte 1 | destination: bits 4-7 (>0x10), source: bits 0-3 (>0x00)
  char payload [29] = "";                      // bytes 2-30
  byte crc          = 0;                       // byte 31
} Pack;

Pack package;

int timeout = 0;
int timeout2 = 0;
bool sent = false;
byte recType = 0;
byte mySn = 0;
byte recSn = 0;
byte prevSn = 0;
byte myLength = 0;


byte recLength = 0;
bool checkLength = true;
bool checkLength2 = true;
byte myDestination = 1; //= 0 para o utilizador 1
byte recDestination = 0;
byte mySource = 0; //= 1 para o utilizador 1
byte recSource = 0;
char myPayload[29] = "";
char recPayload[29] = "";
byte crc = 0;
byte ack = 0;
byte prevAck = 0;
String myPayloadS = "";
bool txMod = false;
long int sentMillis = 0;
int j = 0;
long int totalJ = 0;
char b = 0;

int paylsize = 0;
boolean donewithsize = false;
long int myTotalLength;
char myTotalLengthA[29] = "";
boolean startcounter = false ;
long counter = 0;
boolean allread = false ;
boolean found = false;

void setup() {
  package.typeSnLength = 0x80 * 0 + 0x20 * mySn + 29; // Type = 0, SN = 1, payload length = 29
  package.adresses = 0x10 * myDestination + mySource;             // destination = 0, source = 1
  Serial.begin(9600);
  radio.begin();
  radio.setChannel(85);
  radio.openReadingPipe(mySource, 0xF0F0F0F000 + mySource);
  radio.openWritingPipe(0xF0F0F0F000 + myDestination);
  radio.setPALevel(RF24_PA_LOW);
  radio.setDataRate(RF24_2MBPS);
  radio.setAutoAck(false);
  radio.disableCRC();
  srand(time(0));
}
void loop() {
  //receiver
  txMod = false;
  radio.startListening();
  delay(2);
  if (radio.available()) {
    radio.read(&package, sizeof(package));
    for (int i = 5; i < 7; i ++)
      bitWrite(recSn, i - 5, bitRead(package.typeSnLength, i));
    for (int i = 0; i < 5; i ++)
      bitWrite(recLength, i, bitRead(package.typeSnLength, i));
    for (int i = 4; i < 8; i ++)
      bitWrite(recDestination, i - 4, bitRead(package.adresses, i));
    for (int i = 0; i < 4; i ++)
      bitWrite(recSource, i, bitRead(package.adresses, i));

    if (recDestination == mySource && (prevSn != recSn || !prevAck)) {
      Serial.print(recSn);
      Serial.print("-");
      Serial.print(recSource);
      Serial.print(": ");
      for (int i = 0; i < recLength; i ++) {
        recPayload[i] = package.payload[i];
        Serial.print(recPayload[i]);
      }
    }
    crc = CRC8.smbus(recPayload, recLength);

    if (crc != package.crc) {
      prevAck = false;
      Serial.println(" | Error!");
    }
    else {
      package.typeSnLength = 0x80 * 1 + 0x20 * 0 + 0;
      radio.stopListening();
      radio.setPayloadSize(1);
      radio.write(&package, sizeof(package));
      radio.setPayloadSize(32);
      radio.startListening();
      prevAck = true;
      Serial.println();
    }
    prevSn = recSn;
    memset(recPayload, 0, sizeof(recPayload));
  }
  //sender


    if (Serial.available() && j < 29) {
      myPayload[j] = Serial.read();
      Serial.println(myPayload[j]);
      
      if (checkLength && myPayload[j] == '[') {
        myTotalLength = atol(myTotalLengthA);
        memset(myTotalLengthA,0,sizeof myTotalLengthA);
       // Serial.println("first if");
       // Serial.println(myTotalLength);
        checkLength = false;
        found = true;
      }
      
      if (checkLength) {
        myTotalLengthA[j] = myPayload[j];
       // Serial.println("sc if");
      }
      if (checkLength2 && myPayload[j] == ']') {
        myTotalLength += totalJ;
        //Serial.println("3 if");
        checkLength2 = false;
      }
      package.payload[j] = myPayload[j];
      if (totalJ < myTotalLength + 1) {
       
        j ++;
        totalJ ++;
        //Serial.println("4 if");
      } else if(found){
        found=false;
        //Serial.println("SOU RETARDADO");
        myLength = j;
        j = 0;
        totalJ = 0;
        myTotalLength = 255;
        checkLength = true;
        checkLength2 = true;
        txMod = true;
      }
    }
    if (j == 29) {
      myLength = j;
      j = 0;
      txMod = true;
    }

  
  if (txMod) {
    sent = false;
    if (mySn > 3) mySn = 0;
    package.typeSnLength = 0x80 * 0 + 0x20 * mySn + myLength;         //atualizar o numero de pacote
    package.adresses = 0x10 * myDestination + mySource;
    Serial.print(mySn);
    Serial.print("-");
    Serial.print(mySource);
    Serial.print(": ");
    for (int i = 0; i < myLength; i ++) {
      Serial.print(package.payload[i]);
    }
    while (!sent) {
      package.crc = CRC8.smbus(package.payload, myLength);
      sentMillis = millis();
      radio.stopListening();
      Serial.print("package = ");
      Serial.println(package.payload);
      radio.write(&package, sizeof(package));
      memset(myPayload, 0, sizeof(myPayload));
      radio.startListening();
      timeout = 0;
      while (!sent && timeout < 10) {
        if (radio.available()) {
          radio.startListening();
          radio.read(&package, sizeof(package));
          Serial.print(" | Acknowledged (");
          Serial.print(millis() - sentMillis);
          Serial.println(" ms).");
          radio.stopListening();
          recType = bitRead(package.typeSnLength, 7);
          if (recType) {
            mySn ++;
            recType = 0;
          }
          sent = true;
        }
        else {
          delay (1);
          timeout ++;
          if (timeout == 10);
          Serial.print(" | Resending");
        }
      }
    }
    memset(package.payload, 0, sizeof package.payload);
  }
}
