package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.exception.NotSubscribeException;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.SpringContextUtil;

public class HistoricalBar {
	private Map<Security, Queue<Contract>> map = new ConcurrentHashMap<>();
	private int historicalRange;
	
	private TimeseriesRepository repo;
	
	public HistoricalBar(int historicalRange) {
		this.historicalRange = historicalRange;
		this.repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
	}
	
	public List<Contract> getList(Security security){
		if(!map.containsKey(security)) {
			throw new NotSubscribeException("Doesn't find the security[" + security + "].");
		}
		return map.get(security).stream().collect(Collectors.toList());
	}
	
	public Map<Security, Contract> getEodStatus(){
		Map<Security, Contract> ret = new HashMap<>();
		for (Entry<Security, Queue<Contract>> entry : map.entrySet()) {
			Queue<Contract> q = entry.getValue();
			Contract[] contracts = q.toArray(new Contract[] {});
			ret.put(entry.getKey(), contracts[q.size() - 1]);
		}
		return ret;
	}
	
	public void update(LocalDate tradeDate) {
		map.entrySet().stream().forEach(entry -> {
			Security security = entry.getKey();
			Queue<Contract> q = entry.getValue();
			if(q.size() == historicalRange) {
				q.poll();
			}
			String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			boolean result = getModels(q, security.getName(), tradeDateStr, FreqEnum._1D.getValue());
			if(result) {
				return;
			}
			getModels(q, security.getName(), tradeDateStr, FreqEnum._1MI.getValue());
		});
	}
	
	private boolean getModels(Queue<Contract> q, String security, String tradeDate, String freq){
		List<Timeseries> models = repo.findLatestSecurityByTradeDate(security, tradeDate, freq);
		if(!models.isEmpty()) {
			q.add(new Future(models.get(0)));
			return true;
		}
		return false;
	}
	
	public void subscribe(List<Security> securities, LocalDate tradeDate) {
		map.keySet().retainAll(securities);
		List<Security> copy = new ArrayList<>(securities);
		copy.removeAll(map.keySet());
		copy.stream().forEach(security -> {
			List<Timeseries> list = repo.findByTradeDateWithPeriodLimit(security.getName(), tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "1D", historicalRange);
			int size = list.size();
			Queue<Contract> q = new LinkedBlockingQueue<>();
			for (int i = 0; i < size; i++) {
				Timeseries model = list.get(size - i - 1);
				q.add(new Future(model));
			}
			map.put(security, q);
		});
	}

}
