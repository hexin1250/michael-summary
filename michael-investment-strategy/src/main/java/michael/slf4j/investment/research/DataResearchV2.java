package michael.slf4j.investment.research;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.TopDeal;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.repo.TopDealRepository;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.DataLoaderUtil;
import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.PositionFileUtil;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;
import michael.slf4j.investment.util.TradeUtil;

@Component("dataResearchV2")
public class DataResearchV2 {
	private static final Logger log = Logger.getLogger(DataResearchV2.class);
	private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();
	private static final String FULL = "full";
	private static final String MONTH = "month";
	private static final String WEEK = "week";
	private static final String HIGH = "high";
	private static final String LOW = "low";

	static {
		HEADER_MAP.put("time", "时间");
		HEADER_MAP.put("freq", "周期");
		HEADER_MAP.put("open", "开盘价");
		HEADER_MAP.put("high", "最高价");
		HEADER_MAP.put("low", "最低价");
		HEADER_MAP.put("close", "收盘价");
		HEADER_MAP.put("OI", "OI");
		HEADER_MAP.put("VOLUME", "VOLUME");
		HEADER_MAP.put("MA1", "MA5");
		HEADER_MAP.put("MA2", "MA10");
		HEADER_MAP.put("MA3", "MA20");
		HEADER_MAP.put("MA4", "MA40");
		HEADER_MAP.put("MA5", "MA60");
		HEADER_MAP.put("BOLL LOWER", "BOLL(26,2) LOWER");
		HEADER_MAP.put("BOLL MID", "BOLL(26,2) MID");
		HEADER_MAP.put("BOLL UPPER", "BOLL(26,2) UPPER");
		HEADER_MAP.put("BIAS1", "BIAS(6,12,24) BIAS1");
		HEADER_MAP.put("BIAS2", "BIAS(6,12,24) BIAS2");
		HEADER_MAP.put("BIAS3", "BIAS(6,12,24) BIAS3");
		HEADER_MAP.put("WR1", "WR(10,6,-80,-20) WR1");
		HEADER_MAP.put("WR2", "WR(10,6,-80,-20) WR2");
		HEADER_MAP.put("TR", "ATR(15) TR");
		HEADER_MAP.put("ATR", "ATR(15) ATR");
		HEADER_MAP.put("CCI", "CCI(14)");
		HEADER_MAP.put("MFI", "MFI(14)");
		HEADER_MAP.put("MACD DIFF", "MACD(12,26,9) DIFF");
		HEADER_MAP.put("MACD DEA", "MACD(12,26,9) DEA");
		HEADER_MAP.put("MACD MACD", "MACD(12,26,9) MACD");
		HEADER_MAP.put("K", "KDJ(9,3,3) K");
		HEADER_MAP.put("D", "KDJ(9,3,3) D");
		HEADER_MAP.put("J", "KDJ(9,3,3) J");
		HEADER_MAP.put("RSI1", "RSI(6,12,24) RSI1");
		HEADER_MAP.put("RSI2", "RSI(6,12,24) RSI2");
		HEADER_MAP.put("RSI3", "RSI(6,12,24) RSI3");
	}

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	@Autowired
	private TopDealRepository topDealRepo;

	@Autowired
	MessageService messageService;
	
	@Value("${chat.history.folder}")
	private String folderName;
	
	@Autowired
	private FileService fileService;
	
	private NumberFormat nf;

	public DataResearchV2() {
		this.nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
	}


	public void summarize() {
		summarize("");
	}

	public void summarize(String securityStr) {
		log.info("Start to get new research");
		LocalDateTime current = LocalDateTime.now();
		Variety variety = Variety.RB;
		Timestamp ts = TradeUtil.getTimestamp(current);
		List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1MI.getValue(),
				ts);
		String tTradeDate = lastTradeDates.get(0);
		List<String> securityList = timeseriesRepository.getSecurityList(variety.name(), lastTradeDates.get(0));
		String mainSecurity = null;
		double maxOpenInterest = 0D;
		for (String security : securityList) {
			Double openInterest = timeseriesRepository.getLastOpenInterest(security, ts);
			if (mainSecurity == null) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
			if (openInterest > maxOpenInterest) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
		}
		if (securityStr != null && !securityStr.isBlank()) {
			mainSecurity = securityStr;
		}

		List<StringBuffer> formatList = new ArrayList<StringBuffer>();
		generateTopDeal(current, formatList, variety, mainSecurity, lastTradeDates);

		FreqEnum freq = FreqEnum._15MI;
		StringBuffer sb = new StringBuffer();
		sb.append("下面表格包括了不同周期指标的数据:");
		sb.append("\n");
		sb.append("|");
		sb.append(HEADER_MAP.values().stream().collect(Collectors.joining("|")));
		sb.append("|");
		sb.append("\n");
		formatList.add(sb);

		/**
		 * 15M frequence data
		 */
		List<Timeseries> realTimeList = timeseriesRepository.getAllDataByPeriod(mainSecurity, tTradeDate,
				freq.getValue());
		Map<Timestamp, Timeseries> map = new TreeMap<>();
		for (Timeseries timeseries : realTimeList) {
			map.put(timeseries.getTradeTs(), timeseries);
		}
		List<Timeseries> adjustList = new ArrayList<>();
		for (Entry<Timestamp, Timeseries> entry : map.entrySet()) {
			adjustList.add(entry.getValue());
		}
		Queue<StringBuffer> queue15M = summarizeDataByFreq(freq, current, adjustList, 24);
		queue15M.stream().forEach(currentSb -> formatList.add(currentSb));

		/**
		 * 30M frequence data
		 */
		List<Timeseries> realTimeList30M = DataLoaderUtil.generate30TsListBy15ForRealTime(adjustList);
		Queue<StringBuffer> queue30M = summarizeDataByFreq(FreqEnum._30MI, current, realTimeList30M, 13);

		/**
		 * 1H frequence data
		 */
		List<Timeseries> realTimeList60M = DataLoaderUtil.generate60TsListBy30ForBack(realTimeList30M);
		Queue<StringBuffer> queue60M = summarizeDataByFreq(FreqEnum._1H, current, realTimeList60M, 6);

		/**
		 * 2H frequence data
		 */
		List<Timeseries> realTimeList2H = DataLoaderUtil.generate2HTsListBy30ForBack(realTimeList30M);
//		Queue<StringBuffer> queue2H = summarizeDataByFreq(FreqEnum._2H, current, realTimeList2H, 12);
//		queue2H.stream().forEach(currentSb -> formatList.add(currentSb));

		/**
		 * 1D frequence data
		 */
//		List<Timeseries> realTimeList1D = timeseriesRepository.getAllDataByPeriod(mainSecurity, tTradeDate,
//				FreqEnum._1D.getValue());
		List<Timeseries> realTimeList1D = DataLoaderUtil.generate1DTsListBy30ForBack(realTimeList30M);
//		List<Timeseries> realTimeList1DAdj = adjust(realTimeList1D);
		Queue<StringBuffer> queue1D = summarizeDataByFreq(FreqEnum._1D, current, realTimeList1D, 20);

		/**
		 * 1W frequence data
		 */
		List<Timeseries> realTimeList1W = DataLoaderUtil.generate1WTsListBy1D(realTimeList1D);
		Queue<StringBuffer> queue1W = summarizeDataByFreq(FreqEnum._1W, current, realTimeList1W, 4);

		if (!TradeUtil.isTradingTime()) {
			queue30M.stream().forEach(currentSb -> formatList.add(currentSb));
			queue60M.stream().forEach(currentSb -> formatList.add(currentSb));
			queue1D.stream().forEach(currentSb -> formatList.add(currentSb));
			queue1W.stream().forEach(currentSb -> formatList.add(currentSb));
		}

		generateKeyPoints(formatList, realTimeList1D);
		generateTrail(formatList, mainSecurity, current, realTimeList2H.get(realTimeList2H.size() - 1));

		String fileName = folderName + "/" + tTradeDate + ".question.txt";
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {
			for (StringBuffer strb : formatList) {
				bw.write(strb.toString());
				bw.flush();
			}
			log.info("Latest information is generated");
			fileService.getFileStatus();
		} catch (Exception e) {
			log.error("Error when sending message to topic", e);
		}
	}

	private void generateTopDeal(LocalDateTime current, List<StringBuffer> formatList, Variety variety,
			String mainSecurity, List<String> lastTradeDates) {
		int hour = current.getHour();
		if(hour < 15 || hour > 21) {
			return;
		}
		List<TopDeal> topDeals = topDealRepo.findSecuritiesBySecurities(mainSecurity, lastTradeDates);
		Map<String, Map<String, Map<String, Integer>>> map = new HashMap<>();
		Set<String> set = new LinkedHashSet<>();
		for (TopDeal topDeal : topDeals) {
			String tradeDate = topDeal.getTradeDate();
			if (set.size() == 2 && !set.contains(tradeDate)) {
				continue;
			}
			String type = topDeal.getType();
			Map<String, Map<String, Integer>> typeMap = map.get(type);
			if (typeMap == null) {
				typeMap = new LinkedHashMap<>();
				map.put(type, typeMap);
			}
			String client = topDeal.getClient();
			Map<String, Integer> clientMap = typeMap.get(client);
			if (clientMap == null) {
				clientMap = new HashMap<>();
				typeMap.put(client, clientMap);
			}
			clientMap.put(tradeDate, topDeal.getVolume());
			set.add(tradeDate);
		}

		for (Entry<String, Map<String, Map<String, Integer>>> mapEntry : map.entrySet()) {
			String type = mapEntry.getKey();
			Map<String, Map<String, Integer>> typeMap = mapEntry.getValue();
			StringBuffer sb = new StringBuffer();
			sb.append("下面的表格包括机构近几日").append(type).append("龙虎榜的变化情况(第一行为header):");
			sb.append("\n");
			sb.append("|").append("机构").append("|");
			set.stream().forEach(tradeDate -> sb.append(tradeDate).append("|"));
			sb.append("\n");

			for (Entry<String, Map<String, Integer>> clientEntry : typeMap.entrySet()) {
				String client = clientEntry.getKey();
				Map<String, Integer> tradeDateMap = clientEntry.getValue();
				sb.append("|").append(client).append("|");
				set.stream().forEach(tradeDate -> sb.append(tradeDateMap.get(tradeDate)).append("|"));
				sb.append("\n");
			}
			sb.append("\n");
			formatList.add(sb);
		}
	}

	private void generateKeyPoints(List<StringBuffer> formatList, List<Timeseries> realTimeList) {
		Timeseries lastTs = realTimeList.get(realTimeList.size() - 1);
		String latestTradeDate = lastTs.getTradeDate();
		LocalDate lastLd = TradeUtil.getTradeDate(latestTradeDate);
		Map<String, Map<String, BigDecimal>> map = new HashMap<>();
		for (int i = 0; i < realTimeList.size() - 1; i++) {
			Timeseries ts = realTimeList.get(i);
			updateMap(map, FULL, ts);

			String tradeDate = ts.getTradeDate();
			LocalDate ld = TradeUtil.getTradeDate(tradeDate);
			if (ld.plusDays(35).compareTo(lastLd) >= 0) {
				updateMap(map, MONTH, ts);
			}
			if (ld.plusDays(7).compareTo(lastLd) >= 0) {
				updateMap(map, WEEK, ts);
			}
		}
		StringBuffer sb = generateKeyInfo(map);
		formatList.add(sb);
	}

	private StringBuffer generateKeyInfo(Map<String, Map<String, BigDecimal>> map) {
		StringBuffer sb = new StringBuffer();
		Map<String, BigDecimal> fullMap = map.get(FULL);
		Map<String, BigDecimal> monthMap = map.get(MONTH);
		Map<String, BigDecimal> weekMap = map.get(WEEK);
		sb.append("\n");
		sb.append("历史最高点:").append(fullMap.get(HIGH)).append(",历史最低点:").append(fullMap.get(LOW));
		sb.append("\n");
		sb.append("月内最高点:").append(monthMap.get(HIGH)).append(",月内最低点:").append(monthMap.get(LOW));
		sb.append("\n");
		sb.append("周内最高点:").append(weekMap.get(HIGH)).append(",周内最低点:").append(weekMap.get(LOW));
		sb.append("\n");
		return sb;
	}

	private void updateMap(Map<String, Map<String, BigDecimal>> map, String freq, Timeseries ts) {
		Map<String, BigDecimal> freqMap = getMap(map, freq);
		updateMap(freqMap, ts);
	}

	private Map<String, BigDecimal> getMap(Map<String, Map<String, BigDecimal>> map, String freq) {
		Map<String, BigDecimal> freqMap = map.get(freq);
		if (freqMap == null) {
			freqMap = new LinkedHashMap<String, BigDecimal>();
			freqMap.put(HIGH, new BigDecimal(0));
			freqMap.put(LOW, new BigDecimal("100000000000000000000"));
			map.put(freq, freqMap);
		}
		return freqMap;
	}

	private void updateMap(Map<String, BigDecimal> map, Timeseries ts) {
		BigDecimal high = map.get(HIGH);
		BigDecimal low = map.get(LOW);
		if (high.compareTo(ts.getHigh()) < 0) {
			map.put(HIGH, ts.getHigh());
		}
		if (low.compareTo(ts.getLow()) > 0) {
			map.put(LOW, ts.getLow());
		}
	}

	private void generateTrail(List<StringBuffer> formatList, String mainSecurity, LocalDateTime current,
			Timeseries lastTs) {
		StringBuffer sb = new StringBuffer();
		int closePrice = lastTs.getClose().intValue();
		sb.append("现在时间是").append(TradeUtil.getTimestamp(current)).append(",").append("已经收盘,收盘点位").append(closePrice);
		sb.append("\n");
		sb.append("目前处于移仓换月期\n");
		Map<String, String> map = PositionFileUtil.readPositionData();
		if (!map.isEmpty()) {
			int v = Integer.valueOf(map.get(PositionFileUtil.PRICE));
			sb.append("目前持有").append(map.get(PositionFileUtil.DIRECTION)).append(",");
			sb.append("开仓价").append(v).append(",");
			sb.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%").append(".");
		} else {
			sb.append("目前空仓");
		}
		sb.append("\n");
		if (!TradeUtil.isTradingTime()) {
			sb.append("请根据当前时间的(15M,30M,1H,1D,1W)周期的所有数据指标以及过往的趋势,分析螺纹钢期货");
		} else {
			sb.append("请根据当前时间的(15M)周期的所有数据指标以及过往的趋势,分析螺纹钢期货");
		}
		sb.append(
				"(OI,VOLUME,MA5,MA10,MA20,MA40,MA60,BOLL(26,2),BIAS(6,12,24),WR(10,6,-80,-20),ATR(15),CCI(14),MFI(14),MACD(12,26,9),KDJ(9,3,3),RSI(6,12,24)");
		sb.append("以及过往的趋势,分析螺纹钢期货");
		sb.append(mainSecurity);
		if (current.getHour() >= 23 || current.getHour() <= 8
				|| (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			sb.append("日盘");
		} else if (current.getHour() >= 9 && current.getHour() <= 12) {
			sb.append("剩余日盘");
		} else if (current.getHour() >= 15 && current.getHour() <= 20) {
			sb.append("下一个交易日的夜盘和日盘");
		}
		sb.append("的走势预演,和对应的概率,和关键价位预判.基于当前持仓制定策略.");
		sb.append("分析指标的时候,需标注对应的周期.").append("\n");
//		sb.append("注意:在分析过程中,要分析全部技术指标(请仔细检查).在结果展示中,至少包括以下几点:多周期技术面共振分析,日线级别趋势分析,关键价位预判,主力持仓行为解析,日内走势预演,日内交易策略,量化指标验证矩阵(包括周期/趋势方向[用↓↑表示]/动能强度[用★☆表示]/反转信号​​),多空争夺点位");
		sb.append(
				"注意:在分析过程中,要分析全部技术指标(请仔细检查).在结果展示中,至少包括以下几点:日线级别顶底分析(双顶底,多顶底),多空争夺点位,主力持仓行为解析,日内走势预演,周内基于点位的交易策略");
		sb.append("\n");
		sb.append("数据说明:NA代表当前数据缺失");
		sb.append("\n");
		sb.append("格式说明:不能出现table格式");
		sb.append("\n");
		sb.append("交易时间说明:夜盘21:00-23:00,日盘9:00-10:15,10:30-11:30,13:30-15:00");
		sb.append("\n");
		formatList.add(sb);
	}

	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, LocalDateTime current,
			List<Timeseries> realTimeTsList, int limit) {
		List<Double> opens = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();
		List<Double> closes = new ArrayList<Double>();
		List<Double> lows = new ArrayList<Double>();
		List<Double> volumes = new ArrayList<Double>();
		List<StockData> dataList = new ArrayList<>();
		Queue<StringBuffer> ret = new LinkedBlockingQueue<>();
		
		Timeseries prev = null;
		StringBuffer prevData = null;
		int direction = 0;
		int size = realTimeTsList.size();

		for (int i = 0; i < size; i++) {
			Timeseries ts = realTimeTsList.get(i);
			opens.add(ts.getOpen().doubleValue());
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			volumes.add(ts.getVolume().doubleValue());
			double preClose = 0D;
			if (!dataList.isEmpty()) {
				preClose = dataList.get(dataList.size() - 1).getClose();
			}
			dataList.add(new StockData(ts.getOpen().doubleValue(), ts.getHigh().doubleValue(),
					ts.getLow().doubleValue(), ts.getClose().doubleValue(), ts.getVolume().doubleValue(), preClose));

			Map<String, String> dataMap = new LinkedHashMap<String, String>();

			Timestamp end = ts.getTradeTs();
			Timestamp start = new Timestamp(end.getTime() - freq.getPeriod() * 60L * 1000L);
			LocalDateTime ldt = TradeUtil.getLocalDateTime(end);
			if (freq == FreqEnum._1H && ldt.getHour() == 14) {
				start = new Timestamp(end.getTime() - (3 * 60 - 1) * 60L * 1000L);
			} else if (freq == FreqEnum._2H) {
				if (ldt.getHour() == 11) {
					start = new Timestamp(end.getTime() - (150 - 1) * 60L * 1000L);
				} else if (ldt.getHour() == 15) {
					start = new Timestamp(end.getTime() - (90 - 1) * 60L * 1000L);
				}
			}
			StringBuffer timeSb = new StringBuffer();
			if (end.compareTo(TradeUtil.getTimestamp(current)) >= 0) {
				end = TradeUtil.getTimestamp(current);
			}
			timeSb.append(start).append(" - ").append(end);
			dataMap.put("time", timeSb.toString());
			dataMap.put("freq", freq.getValue());
			dataMap.put("open", nf.format(ts.getOpen()));
			dataMap.put("high", nf.format(ts.getHigh()));
			dataMap.put("low", nf.format(ts.getLow()));
			dataMap.put("close", nf.format(ts.getClose()));
			dataMap.put("OI", nf.format(ts.getOpenInterest()));
			dataMap.put("VOLUME", nf.format(ts.getVolume()));
			Map<String, List<Double>> mas = IndicatorUtils.calculateMA(closes);
			for (Entry<String, List<Double>> entry : mas.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> boll = IndicatorUtils.calculateBOLL(closes, 26, 2);
			for (Entry<String, List<Double>> entry : boll.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			String emaV = "NA";
			if (closes.size() >= 10) {
				double ema10 = IndicatorUtils.calculateEMA(closes, 10);
				emaV = nf.format(ema10);
			}
			dataMap.put("EMA", emaV);

			Map<String, List<Double>> bias = IndicatorUtils.calculateBIAS(closes);
			for (Entry<String, List<Double>> entry : bias.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> wr = IndicatorUtils.calculateWR(highs, lows, closes);
			for (Entry<String, List<Double>> entry : wr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value * -1);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> atr = IndicatorUtils.calculateATR(highs, lows, closes, 15);
			for (Entry<String, List<Double>> entry : atr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}
			List<Double> cci = IndicatorUtils.calculateCCI(highs, lows, closes, 14);
			String cciV = "NA";
			if (!cci.isEmpty()) {
				cciV = nf.format(cci.get(cci.size() - 1));
			}
			dataMap.put("CCI", cciV);

			double mfi14 = IndicatorUtils.calculateMFI(highs, lows, closes, volumes, 14);
			dataMap.put("MFI", nf.format(mfi14));

			String difV = "NA";
			String deaV = "NA";
			String macdV = "NA";
			if (closes.size() >= 26) {
				MACDResult macd = TechnicalIndicator.calculateMACD(closes);
				difV = nf.format(macd.dif);
				deaV = nf.format(macd.dea);
				macdV = nf.format(macd.macd);
			}
			dataMap.put("MACD DIFF", difV);
			dataMap.put("MACD DEA", deaV);
			dataMap.put("MACD MACD", macdV);

			KDJResult kdj = TechnicalIndicator.calculateKDJ(highs, lows, closes);
			dataMap.put("K", nf.format(kdj.k));
			dataMap.put("D", nf.format(kdj.d));
			dataMap.put("J", nf.format(kdj.j));

			String rsiV6 = "NA";
			String rsiV12 = "NA";
			String rsiV24 = "NA";
			if (dataList.size() > 24) {
				RSIResult rsi = StockIndicatorCalculator.calculateRSI(dataList);
				if (rsi.getRsi6() != null && rsi.getRsi12() != null && rsi.getRsi24() != null) {
					rsiV6 = nf.format(rsi.getRsi6());
					rsiV12 = nf.format(rsi.getRsi12());
					rsiV24 = nf.format(rsi.getRsi24());
				}
			}
			dataMap.put("RSI1", rsiV6);
			dataMap.put("RSI2", rsiV12);
			dataMap.put("RSI3", rsiV24);

			StringBuffer dataSb = new StringBuffer();
			dataSb.append("|");
			dataSb.append(HEADER_MAP.keySet().stream().map(key -> dataMap.get(key)).collect(Collectors.joining("|")));
			dataSb.append("|");
			dataSb.append('\n');

			if (ret.size() == limit) {
				ret.poll();
			}
			LocalDateTime currentLDT = TradeUtil.getLocalDateTime(ts.getTradeTs());
			if(freq == FreqEnum._1D) {
				if(size - i <= 5) {
					ret.add(dataSb);
				} else if(prev != null) {
					int currentDir = ts.getClose().compareTo(prev.getClose());
					if(direction == 0 || currentDir == 0) {
						direction = currentDir;
					} else if(direction * currentDir < 0) {
						direction = currentDir;
						ret.add(prevData);
					}
				} else {
					ret.add(dataSb);
				}
				prev = ts;
				prevData = dataSb;
			} else if ((TradeUtil.isTradingTime() && freq == FreqEnum._15MI && current.minusMinutes(150).isBefore(currentLDT))
					|| !TradeUtil.isTradingTime()) {
				ret.add(dataSb);
			}
		}
		return ret;
	}

}
