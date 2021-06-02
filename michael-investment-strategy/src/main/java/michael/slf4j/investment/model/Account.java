package michael.slf4j.investment.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import michael.slf4j.investment.exception.CashNotEnoughException;
import michael.slf4j.investment.exception.InvalidCloseException;
import michael.slf4j.investment.util.DealUtil;

public class Account implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Account.class);
	
	private final double initCash;
	private double cash;
	private Map<String, Position> positionMap = new HashMap<>();
	
	public Account(double cash) {
		this.initCash = cash;
		this.cash = cash;
	}
	
	public void deal(Security security, DirectionEnum dir, double dealPrice, int quantity) throws CashNotEnoughException {
		Position position = positionMap.get(security.getName());
		if(position == null) {
			position = new Position(security);
			positionMap.put(security.getName(), position);
		}
		log.info("Deal " + security + ":" + dir + ":" + dealPrice);
		double expectedCash = 0D;
		switch(dir) {
		case buy_close:
			int maxSellPosition = position.sellPosition();
			if(maxSellPosition < quantity) {
				throw new InvalidCloseException("Max sell position is invalid, max is [" + maxSellPosition + "], but expected is [" + quantity + "].");
			}
			break;
		case sell_close:
			int maxBuyPosition = position.buyPosition();
			if(maxBuyPosition < quantity) {
				throw new InvalidCloseException("Max buy position is invalid, max is [" + maxBuyPosition + "], but expected is [" + quantity + "].");
			}
			break;
			default:
				expectedCash = DealUtil.getMargin(security.getVariety(), dealPrice, quantity);
				break;
		}
		if(expectedCash > cash) {
			throw new CashNotEnoughException(expectedCash + " is needed, but " + cash + " is left.");
		}
		ProfitLoss pnl = position.deal(dir, dealPrice, quantity);
		cash = cash - pnl.getMargin() - pnl.getTransactionCost() + pnl.getPnl();
		if(position.done()) {
			positionMap.remove(security.getName());
		}
	}
	
	public double pnl(Map<String, Contract> status) {
		return total(status) - initCash;
	}
	
	public double total(Map<String, Contract> status) {
		double total = cash;
		for (Entry<String, Position> entry : positionMap.entrySet()) {
			Contract contract = status.get(entry.getKey());
			Position position = entry.getValue();
			total += position.total(contract.getClose());
		}
		return total;
	}

}
