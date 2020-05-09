import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class chatAMUServer {
  //Les clients envoient leur message dans cette queue pour que le dispatcher les envoi aux autres clients
  ArrayBlockingQueue<String> dispatchQueue = new ArrayBlockingQueue<>(512);
  //Map de tous les clients de leur BlockingQueue pour pouvoir leur envoyer des messages
  Map<String, BlockingQueue<String>> clients = new HashMap<>();

  public static void main(String[] args) {
    int argc = args.length;
    chatAMUServer serveur;

    /* Traitement des arguments */
    if (argc == 1) {
      try {
        serveur = new chatAMUServer();
        serveur.demarrer(Integer.parseInt(args[0]));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("Usage: java chatAMUServer @port");
    }
    return;
  }

  public void demarrer(int port){
    ServerSocket socket;

    try{
      socket = new ServerSocket(port);
      socket.setReuseAddress(true);
      new Dispatcher().start();
      while(true){
        new ClientHandler(socket.accept()).start();
      }
    }catch (IOException e){
      System.out.println("Arret anormal du serveur");
      return;
    }
  }
/*  ClientHandler
*
* Gere la connexion d'un client et lui associe un ClientSender
* Envoi au Dispatcher les messages de ce client via la DispatchQueue
*
* */
  class ClientHandler extends Thread{
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    InetAddress hote;
    int port;
    String login;
    BlockingQueue<String> clientBlockingQueue;
    ClientSender sender;

    ClientHandler(Socket socket) throws IOException{
      this.socket = socket;
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      hote = socket.getInetAddress();
      port = socket.getPort();
    }

    public void run(){
      try{
        String buffer = in.readLine();
        if (buffer.split(" ")[0].equals("LOGIN")) {
          login = buffer.split(" ", 2)[1];
          clientBlockingQueue = new ArrayBlockingQueue<>(512);
          clients.put(login, clientBlockingQueue);  //Ajout du client a la liste des clients
          sender = new ClientSender(socket, out, clientBlockingQueue);
          sender.start();

          System.out.println("New client : " + login);
        }else{
          out.print("ERROR LOGIN aborting chatamu protocol");
          socket.close();
          out.close();
          in.close();
          return;
        }

        while(true){
          buffer = in.readLine();
          if(buffer.split(" ")[0].equals("MESSAGE")){
            //System.out.println(login + buffer.split(" ", 2)[1]);
            dispatchQueue.add("MESSAGE "+login + ">"+ buffer.split(" ", 2)[1]);
          }
        }

      }catch (Exception e){
        //Client logout
        System.out.println(login + "  s'est déconnecté");
        dispatchQueue.add("MESSAGE "+login + ">"+ " s'est déconnecté ");
        sender.stop = true;
        return;
      }
    }
  }
/* ClientSender
*
* Envoi au client les message des autres clients, qui sont stockés dans clientBlockingQueue
*
**/
  class ClientSender extends Thread{
    boolean stop = false;
    Socket socket;
    PrintWriter out;
    BlockingQueue<String> clientBlockingQueue;

    public ClientSender(Socket socket, PrintWriter out, BlockingQueue<String> clientBlockingQueue) {
      this.socket = socket;
      this.out = out;
      this.clientBlockingQueue = clientBlockingQueue;
    }

    public void run(){
      try{

        out = new PrintWriter(socket.getOutputStream(), true);
        while(true){
          if(stop){
            return; //Arrete le thread
          }
          String string = clientBlockingQueue.take();
          out.println(string);
        }
      }catch (Exception e){
        e.printStackTrace();
        return;
      }
    }
  }
/* Dispatcher
*
* Prends les message dans la dispatchQueue et les envoie a tous les clients via leur BlockingQueue stocké dans clients
*
* */
  class Dispatcher extends Thread{
    public void run(){
      try {
        String string;
        String sender;
        while (true) {
          string = dispatchQueue.take();
          sender = string.split(">")[0];
          sender = sender.split(" ")[1];
          for (String client : clients.keySet()) {
            if (!client.equals(sender)) { //Ne pas renvoyer son message a un client
              clients.get(client).add(string);
            }
          }
        }
      }catch (InterruptedException e){
        e.printStackTrace();
      }
    }
  }
}

