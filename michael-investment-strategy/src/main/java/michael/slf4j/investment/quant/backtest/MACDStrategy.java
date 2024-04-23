package michael.slf4j.investment.quant.backtest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.DirectionEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.formula.calculator.MACDCalculator;
import michael.slf4j.investment.quant.formula.calculator.MACDCalculator.MACD;
import michael.slf4j.investment.quant.formula.impl.MACDFormula;
import michael.slf4j.investment.quant.strategy.AbstractStrategy;
import michael.slf4j.investment.quant.strategy.IStrategy;

public class MACDStrategy extends AbstractStrategy implements IStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Need to clear before trading.
	 */
	private static final String CHANGE_DOMINATE = "change_dominate";
	/**
	 * need to keep this value.
	 */
	private static final String MAIN_SECURITY = "main_security";
	private static final String OPEN_HANDS = "open_hands";
	private static final String TARGET_VARIETY = "variety";
	private static final String TRADING_DIRECTION = "trading_direction";
	
	private MACDFormula macdFormula;

	@Override
	public void init() {
		context.params.put(TARGET_VARIETY, Variety.J);
		macdFormula = new MACDFormula(8, 21, 8);
	}

	@Override
	public int getHistoricalSize() {
		return 0;
	}

	@Override
	public void before() {
		context.params.remove(CHANGE_DOMINATE);
	}

	@Override
	public void handle(Account acc, Bar bar) {
//		Map<String, Object> params = context.params;
//		if(!params.containsKey(CHANGE_DOMINATE)) {
//			changeDominate(acc, bar);
//			params.put(CHANGE_DOMINATE, true);
//		}
//		LocalDateTime ldt = now();
//		if(ldt.getHour() == 9 && ldt.getMinute() == 5) {
//			Security mainSecurity = (Security) params.get(MAIN_SECURITY);
//			Contract contract = bar.getContract(mainSecurity);
//			double closePrice = contract.getClose();
//			LocalDate tradeDate = tradeDate();
//			MACDCalculator calculator = macdFormula.getModel(mainSecurity, tradeDate);
//			MACD macd = calculator.calc(closePrice);
//			
//			Variety variety = (Variety) params.get(TARGET_VARIETY);
//			DirectionEnum dir = (DirectionEnum) params.get(TRADING_DIRECTION);
//			if((macd.operate() == 1 && (dir == null || dir == DirectionEnum.sell)) || (macd.operate() == -1 && (dir == null || dir == DirectionEnum.buy))) {
//				double total = acc.total(context);
//				int targetQ = (int) (total * variety.getCal() / (contract.getClose() * 30));
//				if(targetQ == 0) {
//					targetQ = 1;
//				}
//				if(macd.operate() == 1 && (dir == null || dir == DirectionEnum.sell)) {
//					if(dir != null) {
//						int closeQ = (int) params.get(OPEN_HANDS);
//						acc.deal(mainSecurity, DirectionEnum.buy_close, closePrice, closeQ);
//						log.info(now() + " " + mainSecurity + ":" + DirectionEnum.buy_close + ":" + closePrice + ":" + closeQ);
//					}
//					acc.deal(mainSecurity, DirectionEnum.buy, closePrice, targetQ);
//					log.info(now() + " " + mainSecurity + ":" + DirectionEnum.buy + ":" + closePrice + ":" + targetQ);
//					params.put(OPEN_HANDS, targetQ);
//					params.put(TRADING_DIRECTION, DirectionEnum.buy);
//					dir = DirectionEnum.buy;
//				} else {
//					if(dir != null) {
//						int closeQ = (int) params.get(OPEN_HANDS);
//						acc.deal(mainSecurity, DirectionEnum.sell_close, closePrice, closeQ);
//						log.info(now() + " " + mainSecurity + ":" + DirectionEnum.sell_close + ":" + closePrice + ":" + closeQ);
//					}
//					acc.deal(mainSecurity, DirectionEnum.sell, closePrice, targetQ);
//					log.info(now() + " " + mainSecurity + ":" + DirectionEnum.sell + ":" + closePrice + ":" + targetQ);
//					params.put(OPEN_HANDS, targetQ);
//					params.put(TRADING_DIRECTION, DirectionEnum.sell);
//					dir = DirectionEnum.sell;
//				}
//			}
//		}
	}

	@Override
	public void after() {
	}
	
	private void changeDominate(Account acc, Bar bar) {
		Map<String, Object> params = context.params;
		Variety variety = (Variety) params.get(TARGET_VARIETY);
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

	@Override
	public List<Security> subscriberList(LocalDate tradeDate) {
		Map<String, Object> params = context.params;
		Variety variety = (Variety) params.get(TARGET_VARIETY);
		return getAllFutures(variety, tradeDate);
	}

}
