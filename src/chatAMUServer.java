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

  ArrayBlockingQueue<String> dispatchQueue = new ArrayBlockingQueue<>(512);
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
      System.out.println("Usage: java EchoServer port");
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
          login = buffer.split(" ", 2)[1] + ">";
          clientBlockingQueue = new ArrayBlockingQueue<>(512);
          clients.put(login, clientBlockingQueue);
          sender = new ClientSender(socket, out, clientBlockingQueue);
          sender.start();
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
            System.out.println(login + buffer.split(" ", 2)[1]);
            dispatchQueue.add(login + buffer.split(" ", 2)[1]);
          }//TODO: logout
        }

      }catch (IOException e){e.printStackTrace();}
    }
  }

  class ClientSender extends Thread{
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
        while(true){
          out.print(clientBlockingQueue.take());
        }
      }catch (InterruptedException e){
        e.printStackTrace();
        return;
      }
    }
  }

  class Dispatcher extends Thread{
    public void run(){
      try {
        String string;
        String sender;
        while (true) {
          string = dispatchQueue.take();
          System.out.println(string);
          sender = string.split(">")[0];
          for (String client : clients.keySet()) {
            if (!client.equals(sender)) {
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

