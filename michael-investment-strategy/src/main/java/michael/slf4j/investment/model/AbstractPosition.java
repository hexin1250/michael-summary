package michael.slf4j.investment.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public abstract class AbstractPosition implements Position, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected final Security security;
	protected Map<DirectionEnum, Queue<DealInfo>> securityMap = new HashMap<>();
	
	public AbstractPosition(Security security) {
		this.security = security;
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
	
	protected int totalQuantity(DirectionEnum direction) {
		if(securityMap.isEmpty() || securityMap.get(direction) == null || securityMap.get(direction).isEmpty()) {
			return 0;
		}
		Queue<DealInfo> queue = securityMap.get(direction);
		return queue.parallelStream().mapToInt(dealInfo -> dealInfo.quantity).sum();
	}
	
	protected static class DealInfo {
		protected final double price;
		protected int quantity;
		public DealInfo(double price, int quantity) {
			this.price = price;
			this.quantity = quantity;
		}
		
		@Override
		public String toString() {
			return price + ":" + quantity;
		}
	}

}
