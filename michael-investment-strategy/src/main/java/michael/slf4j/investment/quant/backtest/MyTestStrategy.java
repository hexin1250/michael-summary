package michael.slf4j.investment.quant.backtest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.DirectionEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.strategy.AbstractStrategy;
import michael.slf4j.investment.quant.strategy.IStrategy;

public class MyTestStrategy extends AbstractStrategy implements IStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String BUY_PRICE = "buy_price";
	private static final String SELL_PRICE = "sell_price";
	private static final String MAIN_SECURITY = "main_security";
	private static final String CHANGE_DOMINATE = "change_dominate";
	
	private Variety variety;
	private List<Security> subscriberList;
	private double range;
	private int dataRange;
	private DirectionEnum dir;
	private double k;
	private LocalDate tradeDate;
	
	public MyTestStrategy() {
		variety = Variety.I;
		dataRange = 5;
		k = 0.4D;
		params.put(Context.HISTORICAL_RANGE, dataRange);
	}

	@Override
	public Map<String, Object> getParams() {
		return params;
	}

	@Override
	public void before(Context context, LocalDate current) {
		params.clear();
		Security security = subscriberList.get(0);
		List<Contract> list = context.historical.getList(security.getName());
		range = getRangeValue(list);
		params.put(MAIN_SECURITY, security);
	}

	@Override
	public void handle(Account acc, Bar bar) {
		if(subscriberList.size() == 2 && !params.containsKey(CHANGE_DOMINATE)) {
			changeDominate(acc, bar);
			params.put(CHANGE_DOMINATE, true);
		}
		Security security = (Security) params.get(MAIN_SECURITY);
		if(!params.containsKey(BUY_PRICE)) {
			Contract contract = bar.getContract(security.getName());
			double openPrice = contract.getOpen();
			double buyLine = openPrice + range * k;
			double sellLine = openPrice - range * k;
			params.put(BUY_PRICE, buyLine);
			params.put(SELL_PRICE, sellLine);
		}
		double buyLine = (double) params.get(BUY_PRICE);
		double sellLine = (double) params.get(SELL_PRICE);
		Contract contract = bar.getContract(security.getName());
		double closePrice = contract.getClose();
		if(closePrice > buyLine && (dir == null || dir == DirectionEnum.sell)) {
			if(dir != null) {
				acc.deal(security, DirectionEnum.buy_close, closePrice, 1);
			}
			acc.deal(security, DirectionEnum.buy, closePrice, 1);
			dir = DirectionEnum.buy;
		} else if(closePrice < sellLine && (dir == null || dir == DirectionEnum.buy)) {
			if(dir != null) {
				acc.deal(security, DirectionEnum.sell_close, closePrice, 1);
			}
			acc.deal(security, DirectionEnum.sell, closePrice, 1);
			dir = DirectionEnum.sell;
		}
	}

	@Override
	public void after(Context context, LocalDate current) {
	}

	@Override
	public List<Security> subscriberList(LocalDate tradeDate) {
		this.tradeDate = tradeDate;
		subscriberList = getMainFutures(variety, tradeDate);
		return subscriberList;
	}

	@Override
	public int getHistoricalSize() {
		return dataRange;
	}
	
	private void changeDominate(Account acc, Bar bar) {
		if(dir != null) {
			Security previousSecurity = subscriberList.get(1);
			Contract previousContract = bar.getContract(previousSecurity.getName());
			Security mainSecurity = subscriberList.get(0);
			Contract mainContract = bar.getContract(mainSecurity.getName());
			
			log.info(tradeDate + ": Start to change Dominate.");
			acc.deal(previousSecurity, dir.getPair(), previousContract.getClose(), 1);
			acc.deal(mainSecurity, dir, mainContract.getClose(), 1);
			log.info(tradeDate + ": Done to change Dominate.");
		}
	}

	private Double getRangeValue(List<Contract> list) {
		List<Contract> modelList = list.subList(0, dataRange - 1);
		double hh = modelList.stream().mapToDouble(m -> m.getHigh()).max().getAsDouble();
		double ll = modelList.stream().mapToDouble(m -> m.getLow()).min().getAsDouble();
		double lc = modelList.stream().mapToDouble(m -> m.getClose()).min().getAsDouble();
		double hc = modelList.stream().mapToDouble(m -> m.getClose()).max().getAsDouble();
		double rangeOfValue = Math.max((hh - lc), (hc - ll));
		return rangeOfValue;
	}

}
