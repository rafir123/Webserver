import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpResponseBuild {

    /**
     * The first line of message sent to the client's standard http response
     * */
    public static void sendResponseLine(String version, Integer code, String status, OutputStream outputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setCode(code);
        httpResponse.setStatus(status);
        httpResponse.setVersion(version);
        stringBuilder.append(httpResponse.getVersion()).append(" ")
                .append(httpResponse.getCode()).append(" ")
                .append(httpResponse.getStatus()).append("\n");
        outputStream.write(stringBuilder.toString().getBytes());
    }

    /**
     * Header information that is sent to the client's standard http response
     * */
    public static void sendResponseHeaders(String contentType, Integer contentLength, OutputStream outputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", String.valueOf(contentLength));

        Set<Map.Entry<String, Object>> entries = headers.entrySet();
        for(Map.Entry<String, Object> entry : entries){
            stringBuilder.append(entry.getKey()).append(":")
                    .append(entry.getValue()).append("\n");
        }
        stringBuilder.append("\n");
        outputStream.write(stringBuilder.toString().getBytes());
    }

    /**
     * Standard HTTP response message sent to the client (successful)
     * */
    public static void sendResponse4Success(String version, Integer code, String status,
                                            String contentType, Integer contentLength,
                                            byte[] bytes, OutputStream outputStream) throws IOException {
        sendResponseLine(version, code, status, outputStream);
        sendResponseHeaders(contentType, contentLength, outputStream);
        outputStream.write(bytes);
        outputStream.flush();
    }

    /**
     * Record the number of uri accesses. If the number exceeds the threshold, put it into the cache
     * */

    public static void cacheCountResolve(String uri, String contentType, byte[] bytes){
        //count visit
        Integer requestCount = Constants.HTTP_REQUEST_COUNT.get(uri);
        if(Objects.isNull(requestCount)){
            requestCount = 1;
            Constants.HTTP_REQUEST_COUNT.put(uri, requestCount);
        }else{
            Constants.HTTP_REQUEST_COUNT.put(uri, requestCount+1);
        }
        // If the count exceed threshold put into cache
        if(requestCount > Constants.HTTP_CACHE_THRESHOLD){
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put(Constants.CONTENT_TYPE, contentType);
            bodyMap.put(Constants.BYTES, bytes);
            Constants.HTTP_BODY_CACHE.put(uri, bodyMap);
        }
    }

    /**
     * method to respond http request
     * */
    public static void sendResponse(HttpRequest httpRequest, OutputStream outputStream) throws IOException {
        if(httpRequest.getHttpMethod().toLowerCase().equals("post")){
                /*If it is a post request, get the specific content of the post request, and then return this specific content*/
            String message = httpRequest.getMessage();
            sendResponseLine("HTTP/1.1", 200, "OK", outputStream);
            sendResponseHeaders("text/plain", message.getBytes().length, outputStream);
            outputStream.write(message.getBytes());
            outputStream.flush();
        }else if(httpRequest.getHttpMethod().toLowerCase().equals("get")){
            /*If it is a request from the get method, you need to find the specific file and return the file content*/
            String uri = httpRequest.getUri();
            String userDir = System.getProperty("user.dir");
            File projectRoot = new File(userDir);
            /*Determine if it exists in the cache, if it exists, get the content directly from the cache, and respond to the client*/
            Map<String,Object> cacheMap= Constants.HTTP_BODY_CACHE.get(uri);
            if(cacheMap != null){
                String contentType = (String)cacheMap.get(Constants.CONTENT_TYPE);
                byte[] bytes = (byte[])cacheMap.get(Constants.BYTES);
                sendResponse4Success("HTTP/1.1", 200, "OK", contentType, bytes.length, bytes, outputStream);
                return;
            }
            /*Determine whether the path is a root path. If it is a root path, obtain it from the root directory (this is similar to tomcat)*/
            if("/".equals(uri) || ("").equals(uri)){
                File[] files = projectRoot.listFiles();
                for(File file:files){
                    if(file.isDirectory() && file.getName().equals("root")){
                        File[] requestFileArray = file.listFiles();
                        for(File fileIndex : requestFileArray){
                            if("index.html".equals(fileIndex.getName()) || "index.htm".equals(fileIndex.getName())){
                                //resovle file index.html
                                if(fileIndex.isFile() && fileIndex.canRead()){
                                    byte[] bytes = Files.readAllBytes(fileIndex.toPath());
                                    String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileIndex.getName());
                                    //Count the number of requests and determine whether to cache the content
                                    cacheCountResolve(uri, contentType, bytes);
                                    sendResponse4Success("HTTP/1.1", 200, "OK", contentType, bytes.length, bytes, outputStream);
                                }else{
                                    sendResponse501(httpRequest, outputStream);
                                }
                            }
                        }
                    }
                }
            }else{
                File fileFromPathArray = null;
                /*Split the url, and then traverse the directories one by one to find files. For example, the url is: path1 / path2 / index.html, you need to get the subfolder path2 from the folder path1
                and looking for file index.html */
                String[] uriArray = uri.substring(1, uri.length()).split("/");
                /*If the length of the url-separated array is only 1, then it must be in the root directory, then look for files from the root directory*/
                if(uriArray.length==1){
                    File[] files = projectRoot.listFiles();
                    List<File> root = Stream.of(files).filter(file -> file.getName().equals("root")).collect(Collectors.toList());
                    if(root!=null && root.size()>0){
                        fileFromPathArray = getFileFromPathArray(root.get(0), uriArray);
                    }else{
                        sendResponse404(httpRequest, outputStream);
                    }
                }else{
                    fileFromPathArray = getFileFromPathArray(projectRoot, uriArray);
                }
                if(fileFromPathArray == null){
                    sendResponse404(httpRequest, outputStream);
                }else{
                    if(fileFromPathArray.isFile() && fileFromPathArray.canRead()){
                        byte[] bytes = Files.readAllBytes(fileFromPathArray.toPath());
                        String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileFromPathArray.getName());
                        //Count the number of requests and determine whether to cache the content
                        cacheCountResolve(uri, contentType, bytes);
                        sendResponse4Success("HTTP/1.1", 200, "OK", contentType, bytes.length, bytes, outputStream);
                    }else{
                        sendResponse501(httpRequest, outputStream);
                    }
                }
            }
        }
    }

    /*Traverse a directory from a url-separated array to find files*/
    public static File getFileFromPathArray(File rootFile, String[] pathArray){
        //Find the folder where the target file is stored from the directory
        File curFile = rootFile;
        int i = 0;
        for (; i < pathArray.length-1; i++) {
            if(curFile.isDirectory()){
                File[] files = curFile.listFiles();
                for(File file:files){
                    if(file.getName().equals(pathArray[i])){
                        curFile = file;
                    }
                }
            }else{
                break;
            }
        }
        //Find the target file from the target folder
        if(curFile.isDirectory()){
            File resultFile = null;
            for(File targetFile : curFile.listFiles()){
                if(targetFile.isFile() && targetFile.getName().equals(pathArray[i])){
                    resultFile = targetFile;
                    break;
                }
            }
            return resultFile;
        }
        return null;
    }

    /**
     * Handling of responses when files do not exist
     * */
    public static void sendResponse404(HttpRequest request, OutputStream outputStream) throws IOException {
        String body = new StringBuilder("<HTML>\r\n")
                .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
                .append("</HEAD>\r\n")
                .append("<BODY>")
                .append("<H1>HTTP Error 404: File Not Found</H1>\r\n")
                .append("</BODY></HTML>\r\n").toString();
        if (request.getVersion().startsWith("HTTP/")) {
            sendResponseLine("HTTP/1.1", 404, "File Not Found", outputStream);
            sendResponseHeaders("text/html; charset=utf-8", body.length(), outputStream);
        }
        //send responseBody
        try(Writer writer = new OutputStreamWriter(outputStream)){
            writer.write(body);
            writer.flush();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Internal error response processing
     * */
    public static void sendResponse501(HttpRequest request, OutputStream outputStream) throws IOException {
        String body = new StringBuilder("<HTML>\r\n")
                .append("<HEAD><TITLE>Not Implemented</TITLE>\r\n")
                .append("</HEAD>\r\n")
                .append("<BODY>")
                .append("<H1>HTTP Error 501: Not Implemented</H1>\r\n")
                .append("</BODY></HTML>\r\n").toString();
        if (request.getVersion().startsWith("HTTP/")) {
            sendResponseLine("HTTP/1.1", 501, "Not Implemented", outputStream);
            sendResponseHeaders("text/html; charset=utf-8", body.length(), outputStream);
        }
        //send responseBody
        try(Writer writer = new OutputStreamWriter(outputStream)){
            writer.write(body);
            writer.flush();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

}
