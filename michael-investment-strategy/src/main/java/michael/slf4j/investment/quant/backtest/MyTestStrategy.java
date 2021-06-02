package michael.slf4j.investment.quant.backtest;

import java.time.LocalDate;
import java.util.List;

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
	private static final String OPEN_HANDS = "open_hands";
	
	private Variety variety;
	private List<Security> subscriberList;
	private double range;
	private int dataRange;
	private DirectionEnum dir;
	private double k;

	@Override
	public void init(Context context) {
		super.init(context);
		variety = Variety.I;
		dataRange = 5;
		k = 0.4D;
		params.put(Context.HISTORICAL_RANGE, dataRange);
	}
	
	@Override
	public void before(Context context, LocalDate current) {
		params.remove(CHANGE_DOMINATE);
		params.remove(MAIN_SECURITY);
		params.remove(BUY_PRICE);
		params.remove(SELL_PRICE);
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
			double total = acc.total(context);
			int targetQ = (int) (total / (contract.getClose() * 30));
			if(dir != null) {
				int closeQ = (int) params.get(OPEN_HANDS);
				acc.deal(security, DirectionEnum.buy_close, closePrice, closeQ);
				log.info(now() + " " + security + ":" + DirectionEnum.buy_close + ":" + closePrice + ":" + closeQ);
			}
			acc.deal(security, DirectionEnum.buy, closePrice, targetQ);
			log.info(now() + " " + security + ":" + DirectionEnum.buy + ":" + closePrice + ":" + targetQ);
			params.put(OPEN_HANDS, targetQ);
			dir = DirectionEnum.buy;
		} else if(closePrice < sellLine && (dir == null || dir == DirectionEnum.buy)) {
			double total = acc.total(context);
			int targetQ = (int) (total / (contract.getClose() * 30));
			if(dir != null) {
				int closeQ = (int) params.get(OPEN_HANDS);
				acc.deal(security, DirectionEnum.sell_close, closePrice, closeQ);
				log.info(now() + " " + security + ":" + DirectionEnum.sell_close + ":" + closePrice + ":" + closeQ);
			}
			acc.deal(security, DirectionEnum.sell, closePrice, targetQ);
			log.info(now() + " " + security + ":" + DirectionEnum.sell + ":" + closePrice + ":" + targetQ);
			params.put(OPEN_HANDS, targetQ);
			dir = DirectionEnum.sell;
		}
	}

	@Override
	public void after(Context context, LocalDate current) {
	}

	@Override
	public List<Security> subscriberList(LocalDate tradeDate) {
		subscriberList = getMainFutures(variety, tradeDate);
		return subscriberList;
	}

	@Override
	public int getHistoricalSize() {
		return dataRange;
	}
	
	private void changeDominate(Account acc, Bar bar) {
		if(dir != null) {
			int quantity = (int) params.get(OPEN_HANDS);
			Security previousSecurity = subscriberList.get(1);
			Contract previousContract = bar.getContract(previousSecurity.getName());
			Security mainSecurity = subscriberList.get(0);
			Contract mainContract = bar.getContract(mainSecurity.getName());
			
			log.info(now() + ": Start to change Dominate.");
			acc.deal(previousSecurity, dir.getPair(), previousContract.getClose(), quantity);
			log.info(now() + " " + previousSecurity + ":" + dir.getPair() + ":" + previousContract.getClose() + ":" + quantity);
			acc.deal(mainSecurity, dir, mainContract.getClose(), quantity);
			log.info(now() + " " + mainSecurity + ":" + dir + ":" + mainContract.getClose() + ":" + quantity);
			log.info(now() + ": Done to change Dominate.");
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
