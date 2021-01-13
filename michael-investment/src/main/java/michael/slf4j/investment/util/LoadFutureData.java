package michael.slf4j.investment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadFutureData {
	private static final Pattern pattern = Pattern.compile("(.*)([\\s]+INFO[\\s]+)");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
	private static final String PREFIX = "I";
	private static final String SQL_TEMPLATE = "insert into timeseries(security,security_name,open,high,low,close,up_limit,down_limit,volume,freq,trade_date,trade_ts,is_main_future) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public static void main(String[] args)
			throws FileNotFoundException, IOException, SQLException, ClassNotFoundException, ParseException {
		String driverClass = "com.mysql.cj.jdbc.Driver";
		Class.forName(driverClass);

		String dirName = "src/main/data/I";
		String url = "jdbc:mysql://CHs-MacBook-Pro.local:3306/investment?useUnicode=true;characterEncoding=utf-8";
		String user = "springuser";
		String password = "springuser";
		int count = 0;
		try (Connection conn = DriverManager.getConnection(url, user, password);
				PreparedStatement ps = conn.prepareStatement(SQL_TEMPLATE)) {
			Set<String> tradeDates = getTradeDates(conn);
			File dir = new File(dirName);
			Map<String, Map<String, TimeseriesModel>> tradeMap = new HashMap<>();
			for (File file : dir.listFiles()) {
				System.out.println("Start to read file[" + file.getName() + "].");
				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
					String line = null;
					while ((line = br.readLine()) != null) {
						String[] parts = line.split("#####");
						Matcher m = pattern.matcher(parts[0]);
						if (m.matches()) {
							String tradeTs = m.group(1);
							Date date = sdf.parse(tradeTs);
							LocalDateTime ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
							String tradeDate = dateSdf.format(date);
							int hour = ld.getHour();
							if(hour > 20) {
								int dw = ld.getDayOfWeek().getValue();
								if(dw == 5) {
									ld = ld.plusDays(3);
								} else {
									ld = ld.plusDays(1);
								}
								tradeDate = ld.format(DateTimeFormatter.ISO_DATE);
							}
							Map<String, TimeseriesModel> tdMap = tradeMap.get(tradeDate);
							if(tdMap == null) {
								if(!tradeDates.contains(tradeDate)) {
									for (Map<String, TimeseriesModel> map : tradeMap.values()) {
										for (TimeseriesModel tm : map.values()) {
											ps.setString(1, tm.getSecurity());
											ps.setString(2, tm.getName());
											ps.setBigDecimal(3, tm.getOpen());
											ps.setBigDecimal(4, tm.getHigh());
											ps.setBigDecimal(5, tm.getLow());
											ps.setBigDecimal(6, tm.getClose());
											ps.setBigDecimal(7, tm.getUpLimit());
											ps.setBigDecimal(8, tm.getDownLimit());
											ps.setBigDecimal(9, tm.getVolume());
											ps.setString(10, "1D");
											ps.setString(11, tm.getTradeDate());
											ps.setString(12, tm.getTradeTs());
											ps.setString(13, tm.getIsMainFuture());
											ps.addBatch();
										}
									}
								}
								tradeMap.clear();
								
								tdMap = new LinkedHashMap<>();
								tradeMap.put(tradeDate, tdMap);
							}
							String mainFuture = parts[1];
							for (int i = 2; i < parts.length; i++) {
								String[] futureParts = parts[i].split(",");
								TimeseriesModel tm = new TimeseriesModel();
								String security = PREFIX + futureParts[0].substring(futureParts[0].length() - 4, futureParts[0].length());
								tm.setSecurity(security);
								tm.setName(futureParts[0]);
								tm.setOpen(new BigDecimal(futureParts[1]));
								tm.setHigh(new BigDecimal(futureParts[2]));
								tm.setLow(new BigDecimal(futureParts[3]));
								tm.setClose(new BigDecimal(futureParts[4]));
								tm.setUpLimit(new BigDecimal(futureParts[5]));
								tm.setDownLimit(new BigDecimal(futureParts[6]));
								tm.setVolume(new BigDecimal(futureParts[7]));
								tm.setTradeDate(tradeDate);
								tm.setTradeTs(tradeTs);
								tm.setFreq("1MI");
								String isMainFuture = null;
								if (security.equals(mainFuture)) {
									isMainFuture = "T";
								} else {
									isMainFuture = "F";
								}
								tm.setIsMainFuture(isMainFuture);
								
								if(!tradeDates.contains(tradeDate)) {
									ps.setString(1, tm.getSecurity());
									ps.setString(2, tm.getName());
									ps.setBigDecimal(3, tm.getOpen());
									ps.setBigDecimal(4, tm.getHigh());
									ps.setBigDecimal(5, tm.getLow());
									ps.setBigDecimal(6, tm.getClose());
									ps.setBigDecimal(7, tm.getUpLimit());
									ps.setBigDecimal(8, tm.getDownLimit());
									ps.setBigDecimal(9, tm.getVolume());
									ps.setString(10, tm.getFreq());
									ps.setString(11, tm.getTradeDate());
									ps.setString(12, tm.getTradeTs());
									ps.setString(13, tm.getIsMainFuture());
									ps.addBatch();
									count++;
									if (count >= 300) {
										ps.executeBatch();
										count = 0;
									}
								}
								
								TimeseriesModel dm = tdMap.get(security);
								if(dm == null) {
									dm = TimeseriesModel.of(tm);
									tdMap.put(security, dm);
								} else {
									dm.setClose(tm.getClose());
									dm.setTradeTs(tm.getTradeTs());
									dm.setVolume(dm.getVolume().add(tm.getVolume()));
									if(dm.getHigh().compareTo(tm.getHigh()) < 0) {
										dm.setHigh(tm.getHigh());
									}
									if(dm.getLow().compareTo(tm.getLow()) > 0) {
										dm.setLow(tm.getLow());
									}
								}
							}
						}
					}
				}
			}
			if (count > 0) {
				ps.executeBatch();
				count = 0;
			}
		}
		System.out.println("Done.");
	}
	
	private static Set<String> getTradeDates(Connection conn) throws SQLException{
		try(PreparedStatement ps = conn.prepareStatement("select distinct trade_date from timeseries")) {
			Set<String> ret = new HashSet<>();
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				ret.add(rs.getString(1));
			}
			return ret;
		}
	}

}
