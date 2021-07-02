package michael.slf4j.investment.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import michael.slf4j.investment.exception.CashNotEnoughException;
import michael.slf4j.investment.exception.InvalidCloseException;
import michael.slf4j.investment.util.DealUtil;

public class Account implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final double initCash;
	private double cash;
	private Map<Security, Position> positionMap;
	private List<Transaction> transactionList;
	
	public Account(double cash) {
		this.initCash = cash;
		this.cash = cash;
		positionMap = new HashMap<>();
		transactionList = new ArrayList<>();
	}
	
	public void deal(Security security, DirectionEnum dir, double dealPrice, int quantity) throws CashNotEnoughException {
		Position position = positionMap.get(security);
		if(position == null) {
			position = new FuturePosition(security);
			positionMap.put(security, position);
		}
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
		Transaction transaction = new Transaction(security, dir, dealPrice, quantity, pnl.getTransactionCost());
		transactionList.add(transaction);
		cash = cash - pnl.getMargin() - pnl.getTransactionCost() + pnl.getPnl();
		if(position.done()) {
			positionMap.remove(security);
		}
	}
	
	public double pnl(Map<Security, Contract> status) {
		return total(status) - initCash;
	}
	
	public double total(Context context) {
		return total(Status.getStatus(context));
	}
	
	public double total(Map<Security, Contract> status) {
		double total = cash;
		for (Entry<Security, Position> entry : positionMap.entrySet()) {
			Contract contract = status.get(entry.getKey());
			Position position = entry.getValue();
			total += position.total(contract.getClose());
		}
		return total;
	}
	
	public Map<Security, Position> getPositions() {
		return positionMap;
	}

	public List<Transaction> getTransactionList() {
		return transactionList;
	}

}
