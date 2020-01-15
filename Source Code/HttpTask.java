import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * HTTP request response task processing class, call every time a request arrives
 * */
public class HttpTask implements Runnable {

    private Socket socket;

    public HttpTask(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        if(socket == null){
            throw new IllegalArgumentException("socket is null!");
        }
        try(OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);){
            //Get request header
            HttpRequest httpRequest = HttpRequestDecoder.parse4HttpRequest(socket.getInputStream());
            try{
                //Process the request and return the response
                HttpResponseBuild.sendResponse(httpRequest, outputStream);
            }catch (Exception e){
                HttpResponseBuild.sendResponse501(httpRequest, outputStream);
            }
            printWriter.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
