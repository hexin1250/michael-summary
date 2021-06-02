package michael.slf4j.investment.quant.strategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.HolidayUtil;
import michael.slf4j.investment.util.SpringContextUtil;
import michael.slf4j.investment.util.TradeUtil;

public abstract class AbstractStrategy implements IStrategy {
	protected final static Logger log = Logger.getLogger(AbstractStrategy.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TimeseriesRepository repo;
	
	protected Map<String, Object> params = new HashMap<>();
	
	public AbstractStrategy() {
		repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
	}
	
	protected final List<Security> getMainFutures(Variety variety, LocalDate tradeDate) {
		LocalDate previousTradeDate = TradeUtil.previousTradeDate(tradeDate);
		while(HolidayUtil.$.isHoliday(previousTradeDate)) {
			previousTradeDate = TradeUtil.previousTradeDate(previousTradeDate);
		}
		List<Timeseries> list = repo.findSecuritiesByVTF(variety.name(), previousTradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "1D");
		int maxOpenInterest = 0;
		String maxSecurity = null;
		String mainSecurity = null;
		for (Timeseries ts : list) {
			int openInterest = ts.getOpenInterest().intValue();
			if("T".equals(ts.getIsMainFuture())) {
				mainSecurity = ts.getSecurity();
			}
			if(openInterest > maxOpenInterest) {
				maxOpenInterest = openInterest;
				maxSecurity = ts.getSecurity();
			}
		}
		List<Security> securities = new ArrayList<>();
		if(mainSecurity.equals(maxSecurity)) {
			securities.add(new Security(mainSecurity, variety));
		} else {
			securities.add(new Security(maxSecurity, variety));
			securities.add(new Security(mainSecurity, variety));
		}
		return securities;
	}
	
	protected final List<String> getAllFutures(Variety variety, LocalDate tradeDate) {
		LocalDate previousTradeDate = TradeUtil.previousTradeDate(tradeDate);
		while(HolidayUtil.$.isHoliday(previousTradeDate)) {
			previousTradeDate = TradeUtil.previousTradeDate(previousTradeDate);
		}
		return repo.findSecurities(variety.name(), previousTradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
	}

}
