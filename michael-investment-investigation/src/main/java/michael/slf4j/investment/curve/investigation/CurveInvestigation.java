package michael.slf4j.investment.curve.investigation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.dbcp2.BasicDataSource;

import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.quant.formula.calculator.KDJCalculator.KDJ;
import michael.slf4j.investment.quant.formula.impl.KDJFormula;

public class CurveInvestigation {
	private static final String url = "jdbc:mysql://localhost:3306/investment?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
	private static final String user = "springuser";
	private static final String password = "springuser";
	private static final String driverClass = "com.mysql.cj.jdbc.Driver";
	
	private static final double THRESHOLD = 0.05D;

	private static final String TS_SQL = "select variety,security,open,high,low,close,open_interest,trade_date,trade_ts from timeseries where freq='1D' and variety=? order by trade_date";

	public static void main(String[] args) throws SQLException {
		/**
		 * Map<Security, Map<TradeDate, Timeseries>>
		 */
		Map<String, Map<String, Timeseries>> securityMap = new TreeMap<>();
		try (BasicDataSource ds = new BasicDataSource()) {
			ds.setUrl(url);
			ds.setUsername(user);
			ds.setPassword(password);
			ds.setDriverClassName(driverClass);
			
			try(Connection conn = ds.getConnection();
					PreparedStatement ps = conn.prepareStatement(TS_SQL)){
				ps.setString(1, "I");
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					Timeseries model = new Timeseries();
					model.setVariety(rs.getString("variety"));
					model.setSecurity(rs.getString("security"));
					model.setClose(rs.getBigDecimal("close"));
					model.setOpen(rs.getBigDecimal("open"));
					model.setHigh(rs.getBigDecimal("high"));
					model.setLow(rs.getBigDecimal("low"));
					model.setOpenInterest(rs.getBigDecimal("open_interest"));
					model.setTradeDate(rs.getString("trade_date"));
					model.setTradeTs(rs.getTimestamp("trade_ts"));
					
					securityMap.putIfAbsent(model.getSecurity(), new TreeMap<>());
					Map<String, Timeseries> tradeDateMap = securityMap.get(model.getSecurity());
					tradeDateMap.put(model.getTradeDate(), model);
				}
			}
		}
		Map<String, Map<String, Map<String, KDJ>>> kdjMap = new TreeMap<>();
		for (Entry<String, Map<String, Timeseries>> securityEntry : securityMap.entrySet()) {
			String security = securityEntry.getKey();
			KDJFormula formula = new KDJFormula(4, 3, 3);
			Map<String, Map<String, KDJ>> ret = formula.getModel(security, securityEntry.getValue().values());
			kdjMap.put(security, ret);
		}
		Map<String, Map<String, DirectionTimeseriesWrapper>> map = investigateMap(securityMap);
		investigateSecurity("I2409", kdjMap, map, securityMap);
	}

	private static void investigateSecurity(String security, Map<String, Map<String, Map<String, KDJ>>> kdjMap,
			Map<String, Map<String, DirectionTimeseriesWrapper>> map, Map<String, Map<String, Timeseries>> securityMap) {
		Map<String, Map<String, KDJ>> kdjTradeMap = kdjMap.get(security);
		Map<String, DirectionTimeseriesWrapper> investTradeMap = map.get(security);
		for (DirectionTimeseriesWrapper model : investTradeMap.values()) {
			String tradeDate = model.getTs().getTradeDate();
			System.out.println(tradeDate + "\t" + model.getStatus() + "->" + model.getTs().getOpen() + "," + model.getTs().getClose() + "," + model.getTs().getHigh() + "," + model.getTs().getLow());
		}
		for (Entry<String, Map<String, KDJ>> tradeEntry : kdjTradeMap.entrySet()) {
			for (Entry<String, KDJ> entry : tradeEntry.getValue().entrySet()) {
				String tradeDate = tradeEntry.getKey();
				KDJ kdj = entry.getValue();
				System.out.println(tradeDate);
				System.out.println("\t\t" + kdj);
				Timeseries model = securityMap.get(security).get(tradeDate);
				System.out.println("\t\t" + model.getOpen() + "," + model.getClose() + "," + model.getHigh() + "," + model.getLow());
			}
		}
	}

	private static Map<String, Map<String, DirectionTimeseriesWrapper>> investigateMap(Map<String, Map<String, Timeseries>> securityMap) {
		Map<String, Map<String, DirectionTimeseriesWrapper>> retMap = new TreeMap<>();
		for (Entry<String, Map<String, Timeseries>> securityEntry : securityMap.entrySet()) {
			/**
			 * 0 -> unknown status
			 * -1 -> short
			 * 1 -> long
			 * 
			 * once threshold reaches to 5%, switch direction
			 */
			int direction = 0;
			/**
			 * [0] is low, [1] is high
			 */
			Timeseries[] tsArr = new Timeseries[2];
			Map<String, DirectionTimeseriesWrapper> tradeDateMap = new TreeMap<>();
			for (Timeseries model : securityEntry.getValue().values()) {
				if(tsArr[0] == null || tsArr[1] == null) {
					tsArr[0] = model;
					tsArr[1] = model;
					continue;
				}
				int ret;
				if(direction == 0) {
					ret = getShortDirection(model, tsArr);
					if(ret == 0) {
						ret = getLongDirection(model, tsArr);
					}
				} else if(direction < 0) {
					ret = getShortDirection(model, tsArr);
				} else {
					ret = getLongDirection(model, tsArr);
				}
				if(ret != 0) {
					if(direction != 0) {
						Timeseries in = tsArr[(direction + 1) / 2];
						tradeDateMap.put(in.getTradeDate(), new DirectionTimeseriesWrapper(in, direction == 1 ? "high" : "low"));
					}
					if(ret == -1) {
						tsArr[0] = model;
					} else {
						tsArr[1] = model;
					}
					direction = ret;
				}
			}
			Timeseries in = tsArr[(direction + 1) / 2];
			tradeDateMap.put(in.getTradeDate(), new DirectionTimeseriesWrapper(in, direction == 1 ? "high" : "low"));
			retMap.put(securityEntry.getKey(), tradeDateMap);
		}
		return retMap;
	}
	
	private static int getShortDirection(Timeseries model, Timeseries[] tsArr) {
		if(model.getLow().doubleValue() < tsArr[0].getLow().doubleValue()) {
			tsArr[0] = model;
		} else if(model.getHigh().doubleValue() > tsArr[0].getLow().doubleValue() * (1 + THRESHOLD)) {
			return 1;
		}
		return 0;
	}
	
	private static int getLongDirection(Timeseries model, Timeseries[] tsArr) {
		if(model.getHigh().doubleValue() > tsArr[1].getHigh().doubleValue()) {
			tsArr[1] = model;
		} else if(model.getLow().doubleValue() * (1 + THRESHOLD) < tsArr[1].getHigh().doubleValue()) {
			return -1;
		}
		return 0;
	}
	
	private static class DirectionTimeseriesWrapper {
		private final Timeseries ts;
		/**
		 * 1 = buy
		 * -1 = sell
		 */
		private final String status;
		public DirectionTimeseriesWrapper(Timeseries ts, String direction) {
			this.ts = ts;
			this.status = direction;
		}
		public Timeseries getTs() {
			return ts;
		}
		public String getStatus() {
			return status;
		}
	}
}
