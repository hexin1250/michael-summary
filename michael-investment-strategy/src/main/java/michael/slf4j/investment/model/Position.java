package michael.slf4j.investment.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import michael.slf4j.investment.exception.EmptySecurityException;
import michael.slf4j.investment.exception.InvalidCloseException;
import michael.slf4j.investment.util.DealUtil;

public class Position implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Security security;
	private Map<DirectionEnum, Queue<DealInfo>> securityMap = new HashMap<>();
	
	public Position(Security security) {
		this.security = security;
	}
	
	/**
	 * return the case we need. If cash is negative, it means that close the position and release the cash.
	 * @param q
	 * @param p
	 * @return
	 * @throws EmptySecurityException 
	 */
	public ProfitLoss deal(DirectionEnum dir, double price, int quantity) {
		double transactionCost = DealUtil.getTransactionCost(security.getVariety(), price, quantity, false);
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
			pnl += (top.price * pair.getValue() + price * dir.getValue()) * diff;
			releaseMargin -= DealUtil.getMargin(security.getVariety(), top.price, diff);
		}
		if(queue.isEmpty()) {
			securityMap.remove(pair);
		}
		return new ProfitLoss(releaseMargin, pnl, transactionCost);
	}
	
	public double pnl(double price) {
		return securityMap.entrySet().stream().mapToDouble(entry -> {
			DirectionEnum dir = entry.getKey();
			Queue<DealInfo> q = entry.getValue();
			return q.stream().mapToDouble(dealInfo -> {
				return (dealInfo.price - price) * dealInfo.quantity * dir.getValue();
			}).sum();
		}).sum();
	}
	
	public double total(double price) {
		return securityMap.entrySet().stream().mapToDouble(entry -> {
			DirectionEnum dir = entry.getKey();
			Queue<DealInfo> q = entry.getValue();
			return q.stream().mapToDouble(dealInfo -> {
				double margin = DealUtil.getMargin(security.getVariety(), dealInfo.price, dealInfo.quantity);
				return (dealInfo.price - price) * dealInfo.quantity * dir.getValue() + margin;
			}).sum();
		}).sum();
	}
	
	public boolean done() {
		return securityMap.isEmpty();
	}
	
	public int buyPosition() {
		return totalQuantity(DirectionEnum.buy);
	}
	
	public int sellPosition() {
		return totalQuantity(DirectionEnum.sell);
	}
	
	private int totalQuantity(DirectionEnum direction) {
		if(securityMap.isEmpty() || securityMap.get(direction) == null || securityMap.get(direction).isEmpty()) {
			return 0;
		}
		Queue<DealInfo> queue = securityMap.get(direction);
		return queue.parallelStream().mapToInt(dealInfo -> dealInfo.quantity).sum();
	}
	
	private static class DealInfo {
		private final double price;
		private int quantity;
		public DealInfo(double price, int quantity) {
			this.price = price;
			this.quantity = quantity;
		}
	}

}
