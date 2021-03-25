import java.io.*;
import java.net.*;
import java.util.*;

class Servidor implements Runnable{
  private static final int PORT = 9988;
  private static Socket cs;
  private static final Mensagem batMensagens = new Mensagem();

  public Servidor(Socket cs){
    this.cs = cs;
  }

  public Socket getOut() throws IOException{
    return this.cs;
  }

  public synchronized void run() {
    //User entra, diz o seu nome e pode falar com users ja existentes
    String name = "";
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(this.cs.getInputStream()));
      PrintWriter NameAsk = new PrintWriter(cs.getOutputStream(), true);
      String message;
      NameAsk.println("Server: What's your nickname? ");
      //batMensagens.putM("Server: What's your nickname? ");
      //User define o seu nome sem restrições
      name = in.readLine();
      batMensagens.putM(name+" joined the chat");
      while ((message = in.readLine()) != null){
        //User manda mensagens, mensagens em branco nao sao enviadas
        if (message.length() > 0) {
          batMensagens.putM(name+": "+message);
        }
      }

    } catch (IOException z) {}
    batMensagens.putM(name+" left the chat");
      //Diminui o numero de threads na class Mensagem

  }


  public static void /*Bat*/main(String args[]) throws IOException {
    ServerSocket ss = new ServerSocket(PORT);
    boolean loop = true;
    //Inicialização de servidor
    System.out.println("Server initialized " + ss.getLocalSocketAddress());
    while (loop) {
      Socket cs = ss.accept();
      //espera utilizadores
      System.out.println("New user connected " + cs.getLocalSocketAddress());
      Servidor a = new Servidor(cs);
      //Criação da thread do utilizador para receber mensagens do mesmo
      Thread joker = new Thread(a);
      //Criação da thread do utilizador para receber mensagens de outros utilizadores
      SendToClient b = new SendToClient(a, batMensagens, joker);
      Thread flash = new Thread(b);
      //Inicio das thread criadas em cima
      flash.start();
      joker.start();
      //define o numero de threads em uso na class Mensagem
      batMensagens.IncrementThread();
    }
    ss.close();
  }


}
