import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class chatAMUClient {

  void start(String ip, int port){

    Socket socket; // la socket client
    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    PrintWriter out;
    BufferedReader in;

    try {
      socket = new Socket(ip, port);
      System.err.println("le n° de la socket est : " + socket);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (IOException e) {
      System.err.println("Connexion: hôte inconnu : " + ip);
      e.printStackTrace();
      return;
    }

    System.out.println("Entrez votre pseudo: ");
    String login = "";
    try {
      login = "LOGIN " + stdin.readLine();
    }catch (IOException e){
      e.printStackTrace();
      System.exit(2);
    }
    out.println(login);

    Listener listener = new Listener(in);
    Sender sender = new Sender(socket, out, stdin);

    sender.start();
    listener.start();

  }

  public static void main(String[] args) throws IOException {
    chatAMUClient client;
    String ip;
    int port = 12345;

    if(args.length == 2){
      port = Integer.parseInt(args[1]);
    }else if (args.length == 1){
      port = 12345;
    }else{
      System.out.println("Usage: java chatAMUClient @server @port");
      System.exit(1);
    }
    ip = args[0];
    if (port > 65535) {
      System.err.println("Port hors limite");
      System.exit(3);
    }

    client = new chatAMUClient();
    client.start(ip, port);
  }

  class Listener extends Thread{

    BufferedReader in;

    public Listener(BufferedReader in) {
      this.in = in;
    }

    public void run(){
      try{
        while(true){
          String recieved;

          recieved = in.readLine();
          String[] strings = recieved.split(" ", 2);
          if(strings[0].equals("ERROR")){
            System.err.println("An error has occurred: " + strings[1]);
            System.exit(10);
          }
          if(strings[0].equals("MESSAGE")){
            System.err.println(strings[1]);
          }

        }
      }catch(IOException e){
        System.exit(2);
      }
    }
  }

  class Sender extends Thread{
    boolean fini = false;
    Socket socket;
    PrintWriter out;
    BufferedReader stdin;

    public Sender( Socket socket, PrintWriter out, BufferedReader stdin) {
      this.socket = socket;
      this.out = out;
      this.stdin = stdin;
    }

    public void run(){
      try{
        while(true){
          if (fini) break;

          String buffer = stdin.readLine();
          if(buffer == null){
            fini = true;
            System.out.println("Closing connection");
            socket.shutdownOutput();
          } else {
            out.println("MESSAGE " + buffer);
          }
        }
        socket.close();
        stdin.close();

      }catch (IOException e){
        System.err.println("Erreur E/S socket");
        e.printStackTrace();
        System.exit(8);}
    }
  }
}
