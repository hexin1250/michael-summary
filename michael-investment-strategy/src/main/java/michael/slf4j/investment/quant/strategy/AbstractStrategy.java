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

import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Account;
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

public abstract class AbstractStrategy implements IStrategy {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected final static Logger log = Logger.getLogger(AbstractStrategy.class);

	private TimeseriesRepository repo;
	
	protected Context context;
	protected Map<Variety, Security> mainSecurityMap = new ConcurrentHashMap<>();
	protected MessageService messageService;
	
	public AbstractStrategy() {
		repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
	}
	
	protected final Security getMainFutures(Variety variety, Bar bar) {
		Security mainSecurity = mainSecurityMap.get(variety);
		double mainOpenInterest = 0D;
		double maxOpenInterest = 0D;
		Security maxSecurity = null;
		for (Entry<Security, Contract> entry : bar.map.entrySet()) {
			Security security = entry.getKey();
			if(security.getVariety() != variety) {
				continue;
			}
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
	
	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}
	
	@Override
	public final void initContext(Context context) {
		this.context = context;
	}
	
	@Override
	public void freq15M(Security security, List<Contract> list) {
	}
	@Override
	public void freq30M(Account acc, Bar bar, Variety variety, Security security, List<Contract> list) {
	}
	@Override
	public void freq60M(Security security, List<Contract> list) {
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
