import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class EchoSelectorServer {

    public static void main(String[] args) {
        int argc = args.length;
        EchoSelectorServer serveur;

        /* Traitement des arguments */
        if (argc == 1) {
            try {
                serveur = new EchoSelectorServer();
                serveur.demarrer(Integer.parseInt(args[0]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Usage: java EchoServer port");
        }
        return;
    }

    void demarrer(int port){
        try {
            FileExport fileExport = new FileExport();

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            ByteBuffer buffer = ByteBuffer.allocate(2048);
            ByteBuffer send = ByteBuffer.allocate(1024);

            while (true) {
                int channelCount = selector.select();
                if (channelCount > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            SocketChannel client = serverSocketChannel.accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ, client.socket().getPort());
                            ByteBuffer buff = Charset.forName("UTF-8").encode("Bonjour " + client.socket().getInetAddress() + "! Vous utilisez le port " + client.socket().getPort() + "\n");
                            client.write(buff);
                            buff.clear();
                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            if (client.read(buffer) < 0) {
                                key.cancel();
                                client.close();
                            } else {
                                buffer.flip();
                                String s = Charset.forName("UTF-8").decode(buffer).toString();
                                long responseTime = System.nanoTime();
                                log(client, s);
                                s = '>' + s ;
                                send.put(Charset.forName("UTF-8").encode(s));
                                send.flip();
                                try {
                                    client.write(send);
                                }catch (IOException e){ //Dans le cas où le client ferme la connection avant que le serveur renvoi l'echo, on a un broken pipe. Dans ce cas on ferme la connection avec le client
                                    client.close();
                                }
                                send.clear();
                                buffer.clear();

                                responseTime = System.nanoTime() - responseTime;
                                fileExport.write(responseTime); //Log du temps de reponse du serveur

                            }
                        }
                    }
                }
            }

            }catch(IOException e){
                e.printStackTrace();
                return;
            }



    }

    private static void log(SocketChannel client, String string){
        System.out.println("[" + client.socket().getInetAddress() + ":" + client.socket().getPort() + "]: " + string);
    }

    class FileExport {

        File file;

        FileExport() {
            try {
                file = new File("serverResponseTime.csv");
                file.delete();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void write(long time) {
            try {
                FileWriter writer = new FileWriter(file, true);

                String string;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(time);
                stringBuilder.append('\n');
                string = stringBuilder.toString();
                writer.write(string);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }
}

