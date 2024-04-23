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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbcp2.BasicDataSource;

import michael.slf4j.investment.model.FutureSecurityEnum;

public class AdvancedLoader {
	private static final Pattern pattern = Pattern.compile("(.*)([\\s]+INFO[\\s]+)");
	private static final String SQL_TEMPLATE = "insert into timeseries(security,security_name,open,high,low,close,up_limit,down_limit,volume,freq,trade_date,trade_ts,variety,open_interest) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String dirName = "src/main/data";
	private static final String url = "jdbc:mysql://localhost:3306/investment?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
	private static final String user = "springuser";
	private static final String password = "springuser";
	private static final String driverClass = "com.mysql.cj.jdbc.Driver";

	private static boolean complete = false;
	private static Map<String, Map<String, Map<String, List<TimeseriesModel>>>> fileMap = new ConcurrentHashMap<>();
	
	private static BasicDataSource ds = null;
	
	static {
		ds = new BasicDataSource();
		ds.setUrl(url);
		ds.setUsername(user);
		ds.setPassword(password);
		ds.setDriverClassName(driverClass);
	}
	
	public static void main(String[] args)
			throws FileNotFoundException, IOException, SQLException, ClassNotFoundException, ParseException, InterruptedException, ExecutionException {
		Class.forName(driverClass);

		try (Connection conn = DriverManager.getConnection(url, user, password)) {
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			MySingleRunnable singleRun = new MySingleRunnable(conn);
			Future<?> f = executorService.submit(singleRun);
			File dir = new File(dirName);
			for (File varietyDir : dir.listFiles()) {
				try {
					String variety = varietyDir.getName();
					Set<String> tradeDates = getTradeDates(conn, variety);
					Arrays.stream(varietyDir.listFiles()).parallel().forEach(file -> {
						Map<String, Map<String, List<TimeseriesModel>>> map = new ConcurrentHashMap<>();
						System.out.println("Start to read file[" + file.getName() + "].");
						String line = null;
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
						try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
							while ((line = br.readLine()) != null) {
								String[] parts = line.split("#####");
								Matcher m = pattern.matcher(parts[0]);
								if (m.matches()) {
									String tradeTs = m.group(1);
									String tradeDate = getTradeDate(sdf, dateSdf, tradeTs);
									if(tradeDates.contains(tradeDate)) {
										continue;
									}
									map.putIfAbsent(tradeDate, new ConcurrentHashMap<>());
									Map<String, List<TimeseriesModel>> securityMap = map.get(tradeDate);
									for (int i = 2; i < parts.length; i++) {
										String[] futureParts = parts[i].split(",");
										if("nan".equalsIgnoreCase(futureParts[1])) {
											continue;
										}
										String security = variety + futureParts[0].substring(futureParts[0].length() - 4, futureParts[0].length());
										if(!FutureSecurityEnum.isTargetSecurity(variety, security)) {
											continue;
										}
										securityMap.putIfAbsent(security, new ArrayList<>());
										List<TimeseriesModel> list = securityMap.get(security);
										TimeseriesModel tm = generateTM(tradeTs, tradeDate, futureParts, security, "1MI");
										list.add(tm);
									}
								}
							}
							fileMap.put(file.getName(), map);
						} catch (Exception e) {
							throw new RuntimeException("Throw Exception as per batch:[" + file.getName() + "]", e);
						}
						System.out.println("Complete to read file[" + file.getName() + "].");
					});
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
			complete = true;
			f.get();
		}
		System.out.println("Done.");
		while(!(complete && fileMap.isEmpty())) {
			continue;
		}
		System.exit(0);
	}

	private static TimeseriesModel generateTM(String tradeTs, String tradeDate, String[] futureParts, String security, String freq) {
		TimeseriesModel tm = new TimeseriesModel();
		tm.setSecurity(security);
		tm.setName(futureParts[0]);
		tm.setOpen(new BigDecimal(futureParts[1]));
		tm.setHigh(new BigDecimal(futureParts[2]));
		tm.setLow(new BigDecimal(futureParts[3]));
		tm.setClose(new BigDecimal(futureParts[4]));
		tm.setUpLimit(new BigDecimal(futureParts[5]));
		tm.setDownLimit(new BigDecimal(futureParts[6]));
		tm.setVolume(new BigDecimal(futureParts[7]));
		tm.setOpenInterest(new BigDecimal(futureParts[8]));
		tm.setTradeDate(tradeDate);
		tm.setTradeTs(tradeTs);
		tm.setFreq(freq);
		return tm;
	}

	private static String getTradeDate(SimpleDateFormat sdf, SimpleDateFormat dateSdf, String tradeTs)
			throws ParseException {
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
		return tradeDate;
	}

	private static void addPsBatch(PreparedStatement ps, TimeseriesModel tm, String freq) throws SQLException {
		ps.setString(1, tm.getSecurity());
		ps.setString(2, tm.getName());
		ps.setBigDecimal(3, tm.getOpen());
		ps.setBigDecimal(4, tm.getHigh());
		ps.setBigDecimal(5, tm.getLow());
		ps.setBigDecimal(6, tm.getClose());
		ps.setBigDecimal(7, tm.getUpLimit());
		ps.setBigDecimal(8, tm.getDownLimit());
		ps.setBigDecimal(9, tm.getVolume());
		ps.setString(10, freq);
		ps.setString(11, tm.getTradeDate());
		ps.setString(12, tm.getTradeTs());
		ps.setString(13, tm.getSecurity().replaceAll("[\\d]+", ""));
		ps.setBigDecimal(14, tm.getOpenInterest());
		ps.addBatch();
	}
	
	private static Set<String> getTradeDates(Connection conn, String variety) throws SQLException{
		try(PreparedStatement ps = conn.prepareStatement("select distinct trade_date from timeseries where variety = ? and freq='1D'")) {
			Set<String> ret = new HashSet<>();
			ps.setString(1, variety);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				ret.add(rs.getString(1));
			}
			return ret;
		}
	}
	
	private static class MySingleRunnable implements Runnable {
		private Connection conn;
		public MySingleRunnable(Connection conn) {
			this.conn = conn;
		}
		
		@Override
		public void run() {
			try {
				ExecutorService executorService = Executors.newFixedThreadPool(20);
				while(!(complete && fileMap.isEmpty())) {
					if(!fileMap.isEmpty()) {
						Iterator<Entry<String, Map<String, Map<String, List<TimeseriesModel>>>>> fileMapIt = fileMap.entrySet().iterator();
						while(fileMapIt.hasNext()) {
							Entry<String, Map<String, Map<String, List<TimeseriesModel>>>> fileEntry = fileMapIt.next();
							String fileName = fileEntry.getKey();
							System.out.println("Start to handle File[" + fileName + "]");
							Map<String, Map<String, List<TimeseriesModel>>> cobMap = fileEntry.getValue();
							List<Future<?>> futureList = new ArrayList<Future<?>>();
							for (Entry<String, Map<String, List<TimeseriesModel>>> cobEntry : cobMap.entrySet()) {
								Map<String, List<TimeseriesModel>> securityMap = cobEntry.getValue();
								for (Entry<String, List<TimeseriesModel>> securityEntry : securityMap.entrySet()) {
									PreparedStatement ps = conn.prepareStatement(SQL_TEMPLATE);
									MyRunnable myRunnable = new MyRunnable(ps, securityEntry.getValue());
									Future<?> futureTask = executorService.submit(myRunnable);
									futureList.add(futureTask);
								}
							}
							for (Future<?> future : futureList) {
								future.get();
							}
							fileMapIt.remove();
							System.out.println("File[" + fileName + "] records have been inserted into DB.");
						}
					}
				}
			} catch(Exception e) {
				throw new RuntimeException("Exception when single thread executing", e);
			}
		}
	}
	
	private static class MyRunnable implements Runnable {
		private List<TimeseriesModel> list;
		private PreparedStatement ps;
		public MyRunnable(PreparedStatement ps, List<TimeseriesModel> list) {
			this.list = list;
			this.ps = ps;
		}
		
		@Override
		public void run() {
			try {
				TimeseriesModel tm1d = null;
				int count = 0;
				for (TimeseriesModel tm : list) {
					count++;
					addPsBatch(ps, tm, tm.getFreq());
					if(count % 300 == 0) {
						ps.executeBatch();
					}
					if(tm1d == null) {
						tm1d = tm;
						tm1d.setFreq("1D");
						tm1d.setVolume(new BigDecimal(0));
					}
					tm1d.setTradeTs(tm.getTradeTs());
					tm1d.setVolume(tm1d.getVolume().add(tm.getVolume()));
					if(tm1d.getHigh().compareTo(tm.getHigh()) < 0) {
						tm1d.setHigh(tm.getHigh());
					}
					if(tm1d.getLow().compareTo(tm.getLow()) > 0) {
						tm1d.setLow(tm.getLow());
					}
					tm1d.setClose(tm.getClose());
				}
				addPsBatch(ps, tm1d, tm1d.getFreq());
				ps.executeBatch();
			} catch (SQLException e) {
				throw new RuntimeException("Exception when executing runnable thread", e);
			}
		}
	}

}
