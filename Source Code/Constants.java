import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constants {

    public static final Integer HTTP_CODE_SUCCESS = 200;

    public static final String CONTENT_TYPE = "contentType";

    public static final String BYTES = "bytes";

    /*HTTP resource access cache threshold*/
    public static Integer HTTP_CACHE_THRESHOLD = 3;

    /*Thread-safe HashMap container, Used to cache the body of frequently accessed resources）,
     key:url，value：map（key：contentType&bytes）*/
    public static Map<String, Map<String, Object>> HTTP_BODY_CACHE = new ConcurrentHashMap<>();

    /*Thread-safe HashMap container for logging the number of accesses per HTTP request，key：url，value：access time*/
    public static Map<String, Integer> HTTP_REQUEST_COUNT = new ConcurrentHashMap<>();


}
