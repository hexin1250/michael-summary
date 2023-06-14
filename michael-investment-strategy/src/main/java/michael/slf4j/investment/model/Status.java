package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Status {
	private static Map<Long, Map<Security, Contract>> barCacheMap = new ConcurrentHashMap<>();
	private static Map<Long, LocalDateTime> tradeTimeMap = new ConcurrentHashMap<>();
	private static Map<Long, LocalDate> tradeDateMap = new ConcurrentHashMap<>();
	
	public static void updateStatus(long runId, Map<Security, Contract> map, LocalDateTime ldt, LocalDate ld) {
		barCacheMap.put(runId, map);
		tradeTimeMap.put(runId, ldt);
		tradeDateMap.put(runId, ld);
	}
	
	public static void updateTime(long runId, LocalDateTime ldt, LocalDate ld) {
		tradeTimeMap.put(runId, ldt);
		tradeDateMap.put(runId, ld);
	}
	
	public static void updateTime(long runId, LocalDateTime ldt) {
		tradeTimeMap.put(runId, ldt);
	}
	
	public static void updateStatus(long runId, Map<Security, Contract> map) {
		barCacheMap.put(runId, map);
	}
	
	public static Map<Security, Contract> getStatus(Context context){
		return getStatus(context.runId);
	}
	
	private static Map<Security, Contract> getStatus(Long runId){
		return barCacheMap.get(runId);
	}
	
	public static LocalDateTime getCurrentTime(Context context) {
		return getCurrentTime(context.runId);
	}
	
	public static LocalDateTime getCurrentTime(Long runId) {
		return tradeTimeMap.get(runId);
	}
	
	public static LocalDate getTradeDate(Context context) {
		return getTradeDate(context.runId);
	}
	
	public static LocalDate getTradeDate(Long runId) {
		return tradeDateMap.get(runId);
	}
	
	public static void unregister(Long runId) {
		barCacheMap.remove(runId);
		tradeTimeMap.remove(runId);
	}

}
