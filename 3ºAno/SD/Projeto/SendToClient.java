import java.io.*;
import java.net.*;
import java.util.*;

class SendToClient implements Runnable {
  Servidor alfred;
  Mensagem batman;
  Thread WonderWoman;

  public SendToClient(Servidor a, Mensagem b, Thread c){
    this.alfred = a;
    this.batman = b;
    this.WonderWoman =  c;
  }

  public void run(){
    try {
       PrintWriter batMessage = new PrintWriter(alfred.getOut().getOutputStream(), true);
       boolean superman = true;
            while (superman) {
              //Thread confirma se ha mensagens por receber para depois mostrar ao user
              String message = batman.getM();
              batMessage.println(message);
              if(!this.WonderWoman.isAlive()){
                //Se o user sair, a thread deixa de existir e fecha
                superman = false;
                batman.DecremenThreads();
              }
            }
        } catch (InterruptedException | IOException e) {}
  }
}
