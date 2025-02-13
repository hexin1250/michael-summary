package michael.slf4j.investment.quant.backtest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

import michael.slf4j.investment.constant.TopicConstants;
import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.DirectionEnum;
import michael.slf4j.investment.model.RealRunTxn;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.strategy.AbstractStrategy;
import michael.slf4j.investment.quant.strategy.IStrategy;

public class ClassicalFutureStrategy extends AbstractStrategy implements IStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Need to clear before trading.
	 */
	private static final String BUY_PRICE = "buy_price";
	private static final String SELL_PRICE = "sell_price";
	private static final String CHANGE_DOMINATE = "change_dominate";
	/**
	 * need to keep this value.
	 */
	private static final String MAIN_SECURITY = "main_security";
	private static final String OPEN_HANDS = "open_hands";
	private static final String K = "k";
	private static final String TRADING_DIRECTION = "trading_direction";
	/**
	 * I/J = 30
	 * RB = 3
	 */
	private static final String OPEN_HANDS_INDEX = "openHandsIndex";
	private static final String VARIETY_MAP = "varietyMap";
	
	@Override
	public void init() {
//		context.params.put(TARGET_VARIETY, Variety.I);
//		context.params.put(K, 0.4D);
//		context.params.put(Context.HISTORICAL_RANGE, 5);
//		context.params.put(TARGET_VARIETY, Variety.J);
//		context.params.put(K, 0.5D);
//		context.params.put(Context.HISTORICAL_RANGE, 4);
//		context.params.put(TARGET_VARIETY, Variety.RB);
//		context.params.put(K, 0.4D);
//		context.params.put(Context.HISTORICAL_RANGE, 5);
//		context.params.put(OPEN_HANDS_INDEX, 3D);
		Map<Variety, Map<String, Object>> contextMap = new HashMap<>();
		Map<String, Object> rbMap = new HashMap<>();
		rbMap.put(K, 0.4D);
		rbMap.put(OPEN_HANDS_INDEX, 3D * 2);
		contextMap.put(Variety.RB, rbMap);
		
		Map<String, Object> iMap = new HashMap<>();
		iMap.put(K, 0.4D);
		iMap.put(OPEN_HANDS_INDEX, 30D * 2);
		contextMap.put(Variety.I, iMap);
		
		context.params.put(VARIETY_MAP, contextMap);
		context.params.put(Context.HISTORICAL_RANGE, 5);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void before() {
		Map<Variety, Map<String, Object>> contextMap = (Map<Variety, Map<String, Object>>) context.params.get(VARIETY_MAP);
		for (Entry<Variety, Map<String, Object>> entry : contextMap.entrySet()) {
			Variety variety = entry.getKey();
			Map<String, Object> varietyMap = entry.getValue();
			if(!varietyMap.containsKey(TRADING_DIRECTION)) {
				RealRunTxn txn = context.getAcc().getLatestTxn(variety.name());
				DirectionEnum dir = context.getAcc().getLatestDirection(txn);
				Integer quatity = context.getAcc().getLatestQuantity(txn);
				Security mainSecurity = context.getAcc().getLatestSecurity(txn);
				if(dir != null) {
					varietyMap.put(TRADING_DIRECTION, dir);
					varietyMap.put(OPEN_HANDS, quatity);
					varietyMap.put(MAIN_SECURITY, mainSecurity);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(Account acc, Bar bar) {
		Map<String, Object> params = context.params;
		Map<Variety, Map<String, Object>> contextMap = (Map<Variety, Map<String, Object>>) params.get(VARIETY_MAP);
		for (Entry<Variety, Map<String, Object>> entry : contextMap.entrySet()) {
			Map<String, Object> varietyMap = entry.getValue();
			Variety variety = entry.getKey();
			dealMarket(acc, bar, variety, varietyMap);
		}
	}

	private void dealMarket(Account acc, Bar bar, Variety variety, Map<String, Object> params) {
		if(!params.containsKey(CHANGE_DOMINATE)) {
			changeVarietyDominate(acc, bar, variety, params);
			params.put(CHANGE_DOMINATE, true);
		}
		Security mainSecurity = (Security) params.get(MAIN_SECURITY);
		if(!params.containsKey(BUY_PRICE)) {
			Contract contract = bar.getContract(mainSecurity);
			double openPrice = contract.getOpen();
			double range = getRangeValue(context.historical.getList(mainSecurity));
			double k = (double) params.get(K);
			double buyLine = openPrice + range * k;
			double sellLine = openPrice - range * k;
			params.put(BUY_PRICE, buyLine);
			params.put(SELL_PRICE, sellLine);
			log.info(now() + " " + mainSecurity.getName() + " Buy line:" + buyLine + ", Sell line:" + sellLine);
			String text = now() + " " + mainSecurity.getName() + " Buy line:" + buyLine + ", Sell line:" + sellLine;
			try {
				messageService.send(TopicConstants.NOTIFICATION_TOPIC, text);
			} catch (JMSException e) {
				log.warn("message to " + TopicConstants.NOTIFICATION_TOPIC + " failed", e);
			}
		}
		double buyLine = (double) params.get(BUY_PRICE);
		double sellLine = (double) params.get(SELL_PRICE);
		Contract contract = bar.getContract(mainSecurity);
		double closePrice = contract.getClose();
		
		DirectionEnum dir = (DirectionEnum) params.get(TRADING_DIRECTION);
		if((closePrice > buyLine && (dir == null || dir == DirectionEnum.sell)) || (closePrice < sellLine && (dir == null || dir == DirectionEnum.buy))) {
			double total = acc.total(context);
			int targetQ = (int) (total / (contract.getClose() * (double)(params.get(OPEN_HANDS_INDEX))));
			if(targetQ == 0) {
				targetQ = 1;
			}
			if(closePrice > buyLine && (dir == null || dir == DirectionEnum.sell)) {
				if(dir != null) {
					int closeQ = (int) params.get(OPEN_HANDS);
					acc.deal(mainSecurity, DirectionEnum.buy_close, closePrice, closeQ);
					log.info(now() + " " + mainSecurity + ":" + DirectionEnum.buy_close + ":" + closePrice + ":" + closeQ);
				}
				acc.deal(mainSecurity, DirectionEnum.buy, closePrice, targetQ);
				log.info(now() + " " + mainSecurity + ":" + DirectionEnum.buy + ":" + closePrice + ":" + targetQ);
				params.put(OPEN_HANDS, targetQ);
				params.put(TRADING_DIRECTION, DirectionEnum.buy);
				dir = DirectionEnum.buy;
			} else {
				if(dir != null) {
					int closeQ = (int) params.get(OPEN_HANDS);
					acc.deal(mainSecurity, DirectionEnum.sell_close, closePrice, closeQ);
					log.info(now() + " " + mainSecurity + ":" + DirectionEnum.sell_close + ":" + closePrice + ":" + closeQ);
				}
				acc.deal(mainSecurity, DirectionEnum.sell, closePrice, targetQ);
				log.info(now() + " " + mainSecurity + ":" + DirectionEnum.sell + ":" + closePrice + ":" + targetQ);
				params.put(OPEN_HANDS, targetQ);
				params.put(TRADING_DIRECTION, DirectionEnum.sell);
				dir = DirectionEnum.sell;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void after() {
		Map<Variety, Map<String, Object>> contextMap = (Map<Variety, Map<String, Object>>) context.params.get(VARIETY_MAP);
		for (Entry<Variety, Map<String, Object>> entry : contextMap.entrySet()) {
			Map<String, Object> varietyMap = entry.getValue();
			varietyMap.remove(CHANGE_DOMINATE);
			varietyMap.remove(BUY_PRICE);
			varietyMap.remove(SELL_PRICE);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Security> subscriberList(LocalDate tradeDate) {
		Map<Variety, Map<String, Object>> contextMap = (Map<Variety, Map<String, Object>>) context.params.get(VARIETY_MAP);
		List<Security> ret = new ArrayList<>();
		for (Variety variety : contextMap.keySet()) {
			ret.addAll(getAllFutures(variety, tradeDate));
		}
		return ret;
	}

	@Override
	public int getHistoricalSize() {
		Map<String, Object> params = context.params;
		return (int) params.get(Context.HISTORICAL_RANGE);
	}

	private void changeVarietyDominate(Account acc, Bar bar, Variety variety, Map<String, Object> params) {
		Security mainSecurity = getMainFutures(variety, bar);
		Security previousSecurity = (Security) params.get(MAIN_SECURITY);
		DirectionEnum dir = (DirectionEnum) params.get(TRADING_DIRECTION);
		if(dir != null && !mainSecurity.equals(previousSecurity)) {
			int quantity = (int) params.get(OPEN_HANDS);
			Contract previousContract = bar.getContract(previousSecurity);
			Contract mainContract = bar.getContract(mainSecurity);
			
			log.info(now() + ": Start to change Dominate.");
			acc.deal(previousSecurity, dir.getPair(), previousContract.getClose(), quantity);
			log.info(now() + " " + previousSecurity + ":" + dir.getPair() + ":" + previousContract.getClose() + ":" + quantity);
			acc.deal(mainSecurity, dir, mainContract.getClose(), quantity);
			log.info(now() + " " + mainSecurity + ":" + dir + ":" + mainContract.getClose() + ":" + quantity);
			log.info(now() + ": Done to change Dominate.");
		}
		params.put(MAIN_SECURITY, mainSecurity);
	}

	private double getRangeValue(List<Contract> list) {
		log.info(now() + "\tCurrent Range list:" + list);
		Map<String, Object> params = context.params;
		int dataRange = (int) params.get(Context.HISTORICAL_RANGE);
		List<Contract> modelList = list.subList(0, dataRange - 1);
		double hh = modelList.stream().mapToDouble(m -> m.getHigh()).max().getAsDouble();
		double ll = modelList.stream().mapToDouble(m -> m.getLow()).min().getAsDouble();
		double lc = modelList.stream().mapToDouble(m -> m.getClose()).min().getAsDouble();
		double hc = modelList.stream().mapToDouble(m -> m.getClose()).max().getAsDouble();
		double rangeOfValue = Math.max((hh - lc), (hc - ll));
		return rangeOfValue;
	}

}
