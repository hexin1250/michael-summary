package michael.slf4j.investment.taskmanager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.util.FutureContract;

@Controller
public class TaskManager {
	private static final Logger log = Logger.getLogger(TaskManager.class);
	
	@Autowired
	private FutureLoader futureLoader;
	
	private ScheduledExecutorService service = Executors.newScheduledThreadPool(20);
	private Map<String, Boolean> recordMap = new ConcurrentHashMap<>();
	private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	
	public Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
	
	public void init() {
		futureLoader.init();
	}
	
	public String subscribeAll() {
		String ret = Constants.VARIETY_LIST.parallelStream().map(variety -> {
			StringBuffer sb = new StringBuffer();
			boolean result = scheduleTask(variety);
			if(result) {
				sb.append("Successful to subscribe [" + variety + "]");
			} else {
				sb.append("Already subscribe [" + variety + "]");
			}
			return sb.toString();
		}).collect(Collectors.joining("<br>"));
		return ret;
	}
	
	public boolean scheduleTask(String variety) {
		if(recordMap.get(variety) == null || !recordMap.get(variety)) {
			recordMap.put(variety, true);
		} else {
			return false;
		}
		List<String> list = FutureContract.getFutureContracts(variety);
		for (String security : list) {
			FutureTask task = new FutureTask(futureLoader, httpClient, variety, security);
			futureMap.put(security, service.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS));
		}
		log.info("schedule tasks have been set for [" + variety + "].");
		return true;
	}
	
	public void cancelTasks() {
		Iterator<Entry<String, ScheduledFuture<?>>> it = futureMap.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, ScheduledFuture<?>> entry = it.next();
			ScheduledFuture<?> future = entry.getValue();
			future.cancel(false);
			while(!future.isDone()) {
				continue;
			}
			it.remove();
		}
		recordMap.clear();
	}
	
	public void fillBack1D() {
		futureLoader.fillBack1D();
	}
	
	public void close() {
		try {
			httpClient.close();
			service.shutdown();
		} catch (IOException e) {
		}
	}
	

}
