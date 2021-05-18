package michael.slf4j.investment.etl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.TimeseriesModel;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.taskmanager.FutureTask;
import michael.slf4j.investment.util.TradeUtil;

@Controller
public class FutureLoader {
	private static final Logger log = Logger.getLogger(FutureTask.class);

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	private Map<String, TimeseriesModel> previousMap = new ConcurrentHashMap<>();
	
	public boolean load(String security, String content) {
		TimeseriesModel m = generateModel(security, content);
		if (TradeUtil.isCompleteMunite()) {
			TimeseriesModel freqTM = m.copy();
			freqTM.setFreq("1MI");
			timeseriesRepository.save(freqTM);
		}
		if (previousMap.get(security) == null || (TradeUtil.isTradingTime() && !m.equals(previousMap.get(security)))) {
			log.info("load[" + security + "] successful.");
			previousMap.put(security, m);
			timeseriesRepository.save(m);
			return true;
		}
		return false;
	}

	private TimeseriesModel generateModel(String security, String content) {
		String[] parts = content.split(",");
		TimeseriesModel m = new TimeseriesModel();
		m.setSecurity(security);
		m.setVariety(security.replaceAll("[\\d]+", ""));
		m.setSecurityName(parts[0]);
		m.setOpen(new BigDecimal(parts[2]));
		m.setHigh(new BigDecimal(parts[3]));
		m.setLow(new BigDecimal(parts[4]));
		m.setClose(new BigDecimal(parts[8]));
		m.setVolume(new BigDecimal(parts[14]));
		BigDecimal buy1 = new BigDecimal(parts[6]);
		BigDecimal sell1 = new BigDecimal(parts[7]);
		if (buy1.compareTo(new BigDecimal(0)) == 0) {
			m.setDownLimit(new BigDecimal(parts[8]));
		}
		if (sell1.compareTo(new BigDecimal(0)) == 0) {
			m.setUpLimit(new BigDecimal(parts[8]));
		}
		m.setFreq("TICK");
		
		m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
		m.setTradeTs(new Timestamp(System.currentTimeMillis()));
		return m;
	}

}
