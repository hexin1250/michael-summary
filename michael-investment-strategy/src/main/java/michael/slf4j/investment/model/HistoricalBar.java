package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import michael.slf4j.investment.exception.NotSubscribeException;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.SpringContextUtil;

public class HistoricalBar {
	private Map<String, Queue<Contract>> map = new HashMap<>();
	private int historicalRange;
	
	private TimeseriesRepository repo;
	
	public HistoricalBar(int historicalRange) {
		this.historicalRange = historicalRange;
		this.repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
	}
	
	public List<Contract> getList(String security){
		if(!map.containsKey(security)) {
			throw new NotSubscribeException("Doesn't find the security[" + security + "].");
		}
		return map.get(security).stream().collect(Collectors.toList());
	}
	
	public Map<String, Contract> getEodStatus(){
		Map<String, Contract> ret = new HashMap<>();
		for (Entry<String, Queue<Contract>> entry : map.entrySet()) {
			Queue<Contract> q = entry.getValue();
			Contract[] contracts = q.toArray(new Contract[] {});
			ret.put(entry.getKey(), contracts[q.size() - 1]);
		}
		return ret;
	}
	
	public void update(LocalDate tradeDate) {
		map.entrySet().stream().forEach(entry -> {
			String security = entry.getKey();
			Queue<Contract> q = entry.getValue();
			if(q.size() == historicalRange) {
				q.poll();
			}
			String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			Timeseries model = repo.findDailySecurityByTradeDate(security, tradeDateStr);
			q.add(new Future(model));
		});
	}
	
	public void subscribe(List<Security> securities, LocalDate tradeDate) {
		List<String> securityStrList = securities.stream().map(security -> security.getName()).collect(Collectors.toList());
		map.keySet().retainAll(securityStrList);
		List<String> copy = new ArrayList<>(securityStrList);
		copy.removeAll(map.keySet());
		copy.stream().forEach(security -> {
			List<Timeseries> list = repo.findByTradeDateWithPeriodLimit(security, tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "1D", historicalRange);
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
