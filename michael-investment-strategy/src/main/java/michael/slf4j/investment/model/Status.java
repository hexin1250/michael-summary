package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Status {
	private static Map<Integer, Map<Security, Contract>> barCacheMap = new ConcurrentHashMap<>();
	private static Map<Integer, LocalDateTime> tradeTimeMap = new ConcurrentHashMap<>();
	private static Map<Integer, LocalDate> tradeDateMap = new ConcurrentHashMap<>();
	
	public static void updateStatus(int runId, Map<Security, Contract> map, LocalDateTime ldt, LocalDate ld) {
		barCacheMap.put(runId, map);
		tradeTimeMap.put(runId, ldt);
		tradeDateMap.put(runId, ld);
	}
	
	public static Map<Security, Contract> getStatus(Context context){
		return getStatus(context.runId);
	}
	
	private static Map<Security, Contract> getStatus(int runId){
		return barCacheMap.get(runId);
	}
	
	public static LocalDateTime getCurrentTime(Context context) {
		return getCurrentTime(context.runId);
	}
	
	private static LocalDateTime getCurrentTime(int runId) {
		return tradeTimeMap.get(runId);
	}
	
	public static LocalDate getTradeDate(Context context) {
		return getTradeDate(context.runId);
	}
	
	private static LocalDate getTradeDate(int runId) {
		return tradeDateMap.get(runId);
	}
	
	public static void unregister(int runId) {
		barCacheMap.remove(runId);
		tradeTimeMap.remove(runId);
	}

}
