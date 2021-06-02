package michael.slf4j.investment.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Status {
	private static Map<Integer, Map<String, Contract>> barCacheMap = new ConcurrentHashMap<>();
	private static Map<Integer, LocalDateTime> tradeTimeMap = new ConcurrentHashMap<>();
	
	public static void updateStatus(int runId, Map<String, Contract> map, LocalDateTime ldt) {
		barCacheMap.put(runId, map);
		tradeTimeMap.put(runId, ldt);
	}
	
	public static Map<String, Contract> getStatus(Context context){
		return getStatus(context.runId);
	}
	
	private static Map<String, Contract> getStatus(int runId){
		return barCacheMap.get(runId);
	}
	
	public static LocalDateTime getCurrentTime(Context context) {
		return getCurrentTime(context.runId);
	}
	
	private static LocalDateTime getCurrentTime(int runId) {
		return tradeTimeMap.get(runId);
	}
	
	public static void unregister(int runId) {
		barCacheMap.remove(runId);
		tradeTimeMap.remove(runId);
	}

}
