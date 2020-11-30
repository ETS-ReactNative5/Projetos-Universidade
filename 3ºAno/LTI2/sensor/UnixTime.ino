#include <WiFi.h>
#include <NTPClient.h>
#include <WiFiUdp.h>

struct tm info;
long int tempounix;
// Define NTP Client to get time
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

// Variables to save date and time
String formattedDate;
String anoS,mesS,diaS,horaS,minutoS,segundoS;
String dayStamp;
String timeStamp;
char formatDate[100];
int ano,mes,dia,hora,minuto,segundo;

void getcharfromdate(){
  for(int i=0;i<formattedDate.length();i++){
     formatDate[i]=formattedDate.charAt(i);
    }
}

long getTime(){
  timeClient.begin();
  
  while(!timeClient.update()) {
    timeClient.forceUpdate();
  }

  formattedDate=timeClient.getFormattedDate();
  getcharfromdate();
  anoS = strtok(formatDate,"-");
  mesS = strtok(&formatDate[5],"-");
  diaS = strtok(&formatDate[8],"T");
  horaS = strtok(&formatDate[11],":");
  minutoS = strtok(&formatDate[14],":");
  segundoS = strtok(&formatDate[17]," ");
  
  ano=anoS.toInt();
  dia=diaS.toInt();
  mes=mesS.toInt();
  hora=horaS.toInt();
  minuto=minutoS.toInt();
  segundo=segundoS.toInt();

   info.tm_year = ano - 1900;
   info.tm_mon = mes - 1;
   info.tm_mday = dia;
   info.tm_hour = hora;
   info.tm_min = minuto;
   info.tm_sec = segundo;
   info.tm_isdst = -1;

  tempounix = mktime(&info);

  return tempounix;
}
