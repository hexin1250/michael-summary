package michael.slf4j.investment.quant.strategy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Status;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.HolidayUtil;
import michael.slf4j.investment.util.SpringContextUtil;
import michael.slf4j.investment.util.TradeUtil;
import michael.slf4j.investment.util.WeChatRobot;

public abstract class AbstractStrategy implements IStrategy {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected final static Logger log = Logger.getLogger(AbstractStrategy.class);

	private TimeseriesRepository repo;
	
	protected Context context;
	protected Map<Variety, Security> mainSecurityMap = new ConcurrentHashMap<>();
	protected WeChatRobot robot;
	
	public AbstractStrategy() {
		repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
		robot = new WeChatRobot();
	}
	
	protected final Security getMainFutures(Variety variety, Bar bar) {
		Security mainSecurity = mainSecurityMap.get(variety);
		double mainOpenInterest = 0D;
		double maxOpenInterest = 0D;
		Security maxSecurity = null;
		for (Entry<Security, Contract> entry : bar.map.entrySet()) {
			Security security = entry.getKey();
			Contract contract = entry.getValue();
			if(contract == null) {
				continue;
			}
			if(security.equals(mainSecurity)) {
				mainOpenInterest = contract.getOpenInterest();
			}
			if(contract.getOpenInterest() > maxOpenInterest) {
				maxOpenInterest = contract.getOpenInterest();
				maxSecurity = security;
			}
		}
		if(maxOpenInterest > 1.1D * mainOpenInterest) {
			mainSecurity = maxSecurity;
		}
		mainSecurityMap.put(variety, mainSecurity);
		return mainSecurity;
	}
	
	protected final List<Security> getAllFutures(Variety variety, LocalDate tradeDate) {
		LocalDate previousTradeDate = TradeUtil.previousTradeDate(tradeDate);
		while(HolidayUtil.$.isHoliday(previousTradeDate)) {
			previousTradeDate = TradeUtil.previousTradeDate(previousTradeDate);
		}
		return repo.findSecurities(variety.name(), previousTradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				.stream().map(security -> new Security(security, variety)).collect(Collectors.toList());
	}
	
	@Override
	public final void initContext(Context context) {
		this.context = context;
	}
	
	public Context getContext() {
		return context;
	}
	
	protected final LocalDateTime now() {
		return Status.getCurrentTime(context);
	}
	
	protected final LocalDate tradeDate() {
		return Status.getTradeDate(context);
	}

}
