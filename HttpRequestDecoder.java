import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * http request Decoder class
 * */
public class HttpRequestDecoder {

    /**
     * Parse the http request header and put the result in the httpRequest parameter
     * */
    public static void decodeRequestHeader(BufferedReader bufferedReader, HttpRequest httpRequest) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        String line = bufferedReader.readLine();
        String[] split;
        while(!"".equals(line)){
            split = line.split(":");
            headers.put(split[0].trim(), split[1].trim());
            line = bufferedReader.readLine();
        }
        httpRequest.setHeaders(headers);
    }

    /**
     * Parse the HTTP request body and put the result in the httpRequest parameter
     * */
    public static void decodeRequestMessage(BufferedReader bufferedReader, HttpRequest httpRequest) throws IOException {
        int contentLength = Integer.parseInt((String) httpRequest.getHeaders().getOrDefault("Content-Length", "0"));
        if(contentLength == 0){
            contentLength = Integer.parseInt((String) httpRequest.getHeaders().getOrDefault("content-length", "0"));
        }
        if(httpRequest.getHttpMethod().toLowerCase().equals("get") && contentLength == 0){
            return;
        }
        char[] messages = new char[contentLength];
        bufferedReader.read(messages);
        httpRequest.setMessage(new String(messages));
    }

    /**
     *Decode the first line of the http request and put the result into the parameter httpRequest
     * */
    public static void decodeRequestLine(BufferedReader bufferedReader, HttpRequest httpRequest) throws IOException {
        String line = bufferedReader.readLine();
        String[] split = line.split(" ");
        httpRequest.setHttpMethod(split[0]);
        httpRequest.setUri(split[1]);
        httpRequest.setVersion(split[2]);
    }

    /**
     * Decode http requests
     * */
    public static HttpRequest parse4HttpRequest(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        HttpRequest httpRequest = new HttpRequest();
        decodeRequestLine(bufferedReader, httpRequest);
        decodeRequestHeader(bufferedReader, httpRequest);
        decodeRequestMessage(bufferedReader, httpRequest);
        return httpRequest;
    }

}
