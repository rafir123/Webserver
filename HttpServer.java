import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpServer {

    private static final int port = 9000;

    public static void main(String[] args){
        //Use multithreading to create a thread pool
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 100, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100), new ThreadPoolExecutor.DiscardPolicy());
        try(ServerSocket serverSocket = new ServerSocket(port);){
            System.out.println("The server started successful...");
            while(true){
                //Wait for the client to connect and return a socket after connecting
                Socket accept = serverSocket.accept();
                //Put into the thread pool to perform request processing tasks
                threadPoolExecutor.submit(new HttpTask(accept));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
