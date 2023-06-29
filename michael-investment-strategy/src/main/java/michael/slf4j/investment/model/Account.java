package michael.slf4j.investment.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import michael.slf4j.investment.exception.CashNotEnoughException;
import michael.slf4j.investment.exception.InvalidCloseException;
import michael.slf4j.investment.repo.RealRunTxnRepository;
import michael.slf4j.investment.util.DealUtil;
import michael.slf4j.investment.util.TradeUtil;
import michael.slf4j.investment.util.WeChatRobot;

public class Account implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private RealRunTxnRepository repo;
	private final long runId;
	private final double initCash;
	private double cash;
	private Map<Security, Position> positionMap;
	private List<RealRunTxn> transactionList;
	private boolean isPersistent = false;
	private WeChatRobot robot;
	
	public Account(long runId, double cash) {
		this.runId = runId;
		this.initCash = cash;
		this.cash = cash;
		positionMap = new HashMap<>();
		transactionList = new ArrayList<>();
		this.robot = new WeChatRobot();
	}
	
	public Account(long runId, double cash, RealRunTxnRepository repo) {
		this(runId, cash);
		this.repo = repo;
	}
	
	public void deal(Security security, DirectionEnum dir, double dealPrice, int quantity) throws CashNotEnoughException {
		String tradeDate = TradeUtil.getDateStr(Status.getTradeDate(runId));
		Timestamp tradeTs = new Timestamp(System.currentTimeMillis());
		deal(security, dir, dealPrice, quantity, tradeDate, tradeTs);
	}

	public void deal(Security security, DirectionEnum dir, double dealPrice, int quantity, String tradeDate, Timestamp tradeTs) throws CashNotEnoughException {
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
		
		RealRunTxn rrt = new RealRunTxn();
		rrt.setRealRunId(runId);
		rrt.setSecurity(security.getName());
		rrt.setVariety(security.getVariety().name());
		BigDecimal dealPriceBd = new BigDecimal(dealPrice);
		rrt.setDealPrice(dealPriceBd);
		rrt.setDealCount(quantity);
		rrt.setDirection(dir.code);
		rrt.setTradeDate(tradeDate);
		rrt.setTradeTs(tradeTs);
//		rrt.setTradeDate(TradeUtil.getDateStr(Status.getTradeDate(runId)));
//		LocalDateTime ldt = Status.getCurrentTime(runId);
//		long timestamp = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();
//		rrt.setTradeTs(new Timestamp(timestamp));

		if(isPersistent) {
			repo.save(rrt);
			robot.sendWechatMessage(rrt.toString());
		}
		
		transactionList.add(rrt);
		cash = cash - pnl.getMargin() - pnl.getTransactionCost() + pnl.getPnl();
		if(position.done()) {
			positionMap.remove(security);
		}
	}
	
	public RealRunTxn getLatestTxn() {
		if(repo == null) {
			return null;
		}
		List<RealRunTxn> list = repo.findByRealRunId(runId);
		RealRunTxn ret = null;
		if(!list.isEmpty()) {
			ret = list.get(list.size() - 1);
		}
		return ret;
	}
	
	public DirectionEnum getLatestDirection(RealRunTxn txn) {
		if(txn != null) {
			int code = txn.getDirection();
			return DirectionEnum.of(code);
		}
		return null;
	}
	
	public Integer getLatestQuantity(RealRunTxn txn) {
		if(txn != null) {
			return txn.getDealCount();
		}
		return null;
	}
	
	public Security getLatestSecurity(RealRunTxn txn) {
		if(txn != null) {
			Variety variety = Variety.of(txn.getVariety());
			return new Security(txn.getSecurity(), variety);
		}
		return null;
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
	
	public String getLatestPositionStatus(Map<Security, Contract> status){
		return positionMap.entrySet().stream().map(entry -> {
			Contract contract = status.get(entry.getKey());
			Security security = entry.getKey();
			double close = contract.getClose();
			Position postion = entry.getValue();
			DirectionEnum direction = postion.getDirection();
			StringBuffer sb = new StringBuffer();
			sb.append(security).append(",").append(direction).append(",").append(close);
			return sb.toString();
		}).collect(Collectors.joining("|"));
	}
	
	public Map<Security, Position> getPositions() {
		return positionMap;
	}

	public List<RealRunTxn> getTransactionList() {
		return transactionList;
	}

	public void setPersistent(boolean isPersistent) {
		this.isPersistent = isPersistent;
	}

}
