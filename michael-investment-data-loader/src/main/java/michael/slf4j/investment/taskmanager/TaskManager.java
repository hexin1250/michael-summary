package michael.slf4j.investment.taskmanager;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.model.SecurityEnum;

@Controller
public class TaskManager {
	private static final Logger log = Logger.getLogger(TaskManager.class);
	
	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	private FutureTask futureTask;
	
	private ScheduledExecutorService service = Executors.newScheduledThreadPool(20);
	private Map<String, Boolean> recordMap = new ConcurrentHashMap<>();
	private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	
	public Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
	
	public String subscribeAll() {
		Set<String> securities = subscribeSecurities();
		String ret = securities.parallelStream().map(security -> {
			StringBuffer sb = new StringBuffer();
			sb.append("Successful to subscribe [" + security + "]");
			return sb.toString();
		}).collect(Collectors.joining("<br>"));
		return ret;
	}
	
	public Set<String> subscribeSecurities() {
		Set<String> securities = new HashSet<>();
		Arrays.stream(SecurityEnum.values()).forEach(e -> {
			securities.addAll(e.getSecurities());
		});
		futureTask.adjustSecurities(securities);
		return securities;
	}
	
	public boolean scheduleTask(Set<String> securities) {
		futureMap.put("futureTask", service.scheduleAtFixedRate(futureTask, 0, 1, TimeUnit.SECONDS));
		log.info("schedule tasks have been set for [futureTask].");
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
		futureTask.clear();
		log.info("Cancel Tasks.");
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
