package michael.slf4j.investment.model;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import michael.slf4j.investment.exception.EmptySecurityException;
import michael.slf4j.investment.exception.InvalidCloseException;
import michael.slf4j.investment.util.DealUtil;

public class FuturePosition extends AbstractPosition implements Position, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FuturePosition(Security security) {
		super(security);
	}
	
	/**
	 * return the case we need. If cash is negative, it means that close the position and release the cash.
	 * @param q
	 * @param p
	 * @return
	 * @throws EmptySecurityException 
	 */
	public ProfitLoss deal(DirectionEnum dir, double price, int quantity) {
		Variety variety = security.getVariety();
		double transactionCost = DealUtil.getTransactionCost(variety, price, quantity, false);
		switch(dir) {
		case buy:
		case sell:
			DealInfo deal = new DealInfo(price, quantity);
			Queue<DealInfo> queue = securityMap.get(dir);
			if(queue == null) {
				queue = new LinkedBlockingQueue<>();
				securityMap.put(dir, queue);
			}
			queue.add(deal);
			double margin = DealUtil.getMargin(security.getVariety(), price, quantity);
			return new ProfitLoss(margin, 0D, transactionCost);
			default:
		}
		DirectionEnum pair = dir.getPair();
		Queue<DealInfo> queue = securityMap.get(pair);
		if(queue == null || queue.isEmpty()) {
			throw new EmptySecurityException("There is no security for [" + pair + "].");
		}
		int maxQuantity = totalQuantity(pair);
		if(maxQuantity < quantity) {
			throw new InvalidCloseException("Close quantity[" + quantity + "] exceed current [" + maxQuantity + "].");
		}
		double releaseMargin = 0D;
		double pnl = 0D;
		while(!queue.isEmpty() && quantity > 0) {
			DealInfo top = queue.peek();
			int diff = 0;
			if(top.quantity <= quantity) {
				diff = top.quantity;
				quantity -= diff;
				queue.poll();
			} else {
				diff = quantity;
				top.quantity = top.quantity - diff;
				quantity = 0;
			}
			pnl += (top.price * pair.getValue() + price * dir.getValue()) * diff * variety.getUnit();
			releaseMargin -= DealUtil.getMargin(security.getVariety(), top.price, diff);
		}
		if(queue.isEmpty()) {
			securityMap.remove(pair);
		}
		return new ProfitLoss(releaseMargin, pnl, transactionCost);
	}
	
	public double total(double price) {
		return securityMap.entrySet().stream().mapToDouble(entry -> {
			DirectionEnum dir = entry.getKey();
			Queue<DealInfo> q = entry.getValue();
			return q.stream().mapToDouble(dealInfo -> {
				Variety variety = security.getVariety();
				double margin = DealUtil.getMargin(variety, dealInfo.price, dealInfo.quantity);
				double pnl = (dealInfo.price - price) * dealInfo.quantity * dir.getValue() * variety.getUnit();
				return pnl + margin;
			}).sum();
		}).sum();
	}

}
