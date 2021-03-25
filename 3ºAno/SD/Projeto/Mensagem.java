import java.io.*;
import java.net.*;
import java.util.*;

class Mensagem {
  private static int NThreads;
  public Vector<String> Messages;
  private static int helper;
  public Mensagem(){
    NThreads =0;
    Messages = new Vector<String>();
    helper = 0;
  }

  public synchronized void putM(String a){
    //Mensagem enviada por um user e colocada na stack
    Messages.addElement(a);
    //Avisa as threads em espera que ha mensagem
    notifyAll();
  }

  public synchronized String getM() throws InterruptedException{

    while(Messages.size() == 0 ){
      //Espera que seja colocada uma mensagem na stack
      wait();
    }
    //Quando ha mensagem e copiada para ser devolvida
    String message = Messages.firstElement();
    //Confirmaçao de que todas as threads copiam a mensagem para poderem enviar ao user
    if(helper < NThreads-1){
      helper++;
      wait();
      //Threads esperam que todos tenham a mensagem
    }else{
      notifyAll();
      //Ultima thread remove a mensagem da stack e desbloqueia as outras
      Messages.removeElement(message);
    }
    helper=0;
    return message;
  }
  public void IncrementThread(){
    NThreads++;
  }
  public void DecremenThreads(){
    NThreads--;
  }
}
