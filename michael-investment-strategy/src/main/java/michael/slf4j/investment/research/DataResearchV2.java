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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
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
	
	public void summarize(Variety variety, boolean fullRequired) {
		summarize(variety, fullRequired, null);
	}

	public void summarize(Variety variety, boolean fullRequired, String researchFileName) {
		log.info("Start to get new research");
		LocalDateTime current = LocalDateTime.now();
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

		List<StringBuffer> formatList = new ArrayList<StringBuffer>();
		generateTopDeal(current, formatList, variety, mainSecurity, tTradeDate);

		FreqEnum freq = FreqEnum._15MI;
		StringBuffer sb = new StringBuffer();
		sb.append("下面表格包括了不同周期指标的数据:");
		sb.append("\n");
		sb.append("|");
		sb.append(HEADER_MAP.values().stream().collect(Collectors.joining("|")));
		sb.append("|");
		sb.append("\n");
		formatList.add(sb);

		String parameter = null;
		if(!fullRequired) {
			parameter = tTradeDate;
		}
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
		Queue<StringBuffer> queue15M = summarizeDataByFreq(freq, current, adjustList, 24, parameter);
		queue15M.stream().forEach(currentSb -> formatList.add(currentSb));

		/**
		 * 30M frequence data
		 */
		List<Timeseries> realTimeList30M = DataLoaderUtil.generate30TsListBy15ForRealTime(adjustList);
		Queue<StringBuffer> queue30M = summarizeDataByFreq(FreqEnum._30MI, current, realTimeList30M, 13, parameter);

		/**
		 * 1H frequence data
		 */
		List<Timeseries> realTimeList60M = DataLoaderUtil.generate60TsListBy30ForBack(realTimeList30M);
		Queue<StringBuffer> queue60M = summarizeDataByFreq(FreqEnum._1H, current, realTimeList60M, 6, parameter);

		/**
		 * 1D frequence data
		 */
		List<Timeseries> realTimeList1D = DataLoaderUtil.generate1DTsListBy30ForBack(realTimeList30M);
		Queue<StringBuffer> queue1D = summarizeDataByFreq(FreqEnum._1D, current, realTimeList1D, 20, parameter);

		/**
		 * 1W frequence data
		 */
		List<Timeseries> realTimeList1W = DataLoaderUtil.generate1WTsListBy1D(realTimeList1D);
		Queue<StringBuffer> queue1W = summarizeDataByFreq(FreqEnum._1W, current, realTimeList1W, 10, parameter);

		queue30M.stream().forEach(currentSb -> formatList.add(currentSb));
		queue60M.stream().forEach(currentSb -> formatList.add(currentSb));
		queue1D.stream().forEach(currentSb -> formatList.add(currentSb));
		queue1W.stream().forEach(currentSb -> formatList.add(currentSb));

		generateKeyPoints(formatList, realTimeList1D);
		generateTrail(variety.name(), tTradeDate, lastTradeDates.get(1), formatList, mainSecurity, current, adjustList.get(adjustList.size() - 1));

		String fileName = null;
		if(researchFileName != null) {
			fileName = researchFileName;
		} else {
			fileName = folderName + "/" + variety.name() + "/" + tTradeDate + ".question.txt";
		}
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {
			for (StringBuffer strb : formatList) {
				bw.write(strb.toString());
				bw.flush();
			}
			log.info("Latest information is generated");
			fileService.getFileStatus(variety);
		} catch (Exception e) {
			log.error("Error when sending message to topic", e);
		}
	}

	private void generateTopDeal(LocalDateTime current, List<StringBuffer> formatList, Variety variety,
			String mainSecurity, String tradeDate) {
		int hour = current.getHour();
		if (hour < 15 || hour > 21) {
			return;
		}
		List<TopDeal> topDeals = topDealRepo.findSecuritiesBySecurities(mainSecurity, tradeDate);
		Map<String, List<TopDeal>> map = new HashMap<>();
		for (TopDeal topDeal : topDeals) {
			String type = topDeal.getType();
			List<TopDeal> list = map.get(type);
			if(list == null) {
				list = new ArrayList<>();
				map.put(type, list);
			}
			list.add(topDeal);
		}
		for (Entry<String, List<TopDeal>> entry : map.entrySet()) {
			String type = entry.getKey();

			StringBuffer sb = new StringBuffer();
			sb.append("下面的表格包括").append(mainSecurity).append("机构今日【").append(type).append("】的变化情况(第一行为header):");
			sb.append("\n");
			sb.append("|机构|名次|净量|增减|");
			sb.append("\n");

			List<TopDeal> deals = entry.getValue();
			int sumVolume = 0;
			int sumOffset = 0;
			for (TopDeal topDeal : deals) {
				sumVolume += topDeal.getVolume();
				sumOffset += topDeal.getOffset();
				sb.append("|").append(topDeal.getClient()).append("|").append(topDeal.getTop()).append("|")
						.append(topDeal.getVolume()).append("|").append(topDeal.getOffset()).append("|");
				sb.append("\n");
			}
			sb.append("\n");
			sb.append(type).append("总持仓:").append(sumVolume).append(",总增减:").append(sumOffset);
			sb.append("\n");
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

	private void generateTrail(String varietyStr, String tDate, String tMinusDate, List<StringBuffer> formatList, String mainSecurity, LocalDateTime current,
			Timeseries lastTs) {
		StringBuffer sb = new StringBuffer();
		int closePrice = lastTs.getClose().intValue();
		sb.append("现在时间是").append(TradeUtil.getTimestamp(current)).append(",");
		if(!TradeUtil.isTradingTime()) {
			sb.append("已经收盘,收盘点位").append(closePrice);
		} else {
			sb.append("交易还在进行中,当前点位").append(closePrice);
		}
		sb.append("\n");
		sb.append("分析螺纹钢期货");
		sb.append(mainSecurity);
		if (current.getHour() >= 23 || current.getHour() <= 8
				|| (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			sb.append("日盘");
		} else if (current.getHour() >= 9 && current.getHour() <= 12) {
			sb.append("剩余日盘");
		} else if (current.getHour() >= 15 && current.getHour() <= 20) {
			sb.append("下一个交易日的夜盘和日盘");
		} else if(current.getHour() == 22) {
			sb.append("剩余夜盘以及次日日盘");
		}
		sb.append("的走势预演,和对应的概率,和关键价位预判\n");
		Map<String, String> map = PositionFileUtil.readPositionData(varietyStr);
		StringBuffer anotherCase = new StringBuffer();
		if (!map.isEmpty()) {
			int v = Integer.valueOf(map.get(PositionFileUtil.PRICE));
			int direction = Integer.valueOf(map.get(PositionFileUtil.DIRECTION_INT));
			anotherCase.append(map.get(PositionFileUtil.DIRECTION));
			anotherCase.append("开仓价").append(v).append(",");
			anotherCase.append("浮");
			if(direction * (closePrice - v) > 0) {
				anotherCase.append("盈");
			} else {
				anotherCase.append("亏");
			}
			anotherCase.append(Math.abs(closePrice - v)).append("点,");
			anotherCase.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%");
		}
		sb.append("针对以下情况制定交易策略(");
		if(anotherCase.isEmpty()) {
			sb.append("空仓");
		} else {
			sb.append(anotherCase);
		}
		sb.append(")\n");
		
		String command = """
第一步,请首先确认的所有数据表格和指标清单:
1. 净多头龙虎榜:机构/名次/净量/增减
2. 净空头龙虎榜:机构/名次/净量/增减
3. 周期指标表:必须包括时间/周期/开盘价/最高价/最低价/收盘价/OI/VOLUME,以及以下所有技术指标(共21个):
 - 趋势指标:MA5, MA10, MA20, MA40, MA60
 - 通道指标:BOLL LOWER, BOLL MID, BOLL UPPER
 - 震荡/动量指标:BIAS1, BIAS2, BIAS3;WR1, WR2;CCI(14);MFI(14);RSI1, RSI2, RSI3
 - 趋势振荡指标:MACD DIFF, MACD DEA, MACD MACD
 - 随机指标:KDJ K, KDJ D, KDJ J
 - 波动指标:ATR TR, ATR ATR
(请在此条后回复"已确认数据清单"后再进行下一步)
第二步,请确认所有数据清单都将会被用作分析
(请在此条后回复"已确认所有指标都将被分析"后再进行下一步)
第三步:逐项分析
请按以下结构分析,每一项都必须明确引用上一步列出的具体指标名称和最新数值:
1.如果这不是最开始的会话,先总结截至目前的走势是否符合2026-02-05的预测
2.日线级别顶底分析:基于日线(1D周期)的开盘价/最高价/最低价/收盘价,结合所有21个形态指标,判断是否存在双顶/双底/头肩等形态,并描述价格相对位置
3.多空争夺点位:确定关键压力和支撑位
4.主力持仓行为解析:对比两张龙虎榜的"总持仓"/"总增减",推断主力意图
5.日内走势预演:综合多周期指标状态,推演夜盘和日盘的可能路径,并给出收盘价预判区间
6.交易策略:基于以上所有分析,为给定的持仓情况,明确给出包含仓位/方向/开/平仓点位/止盈点/止损点的具体计划
第四步:交叉验证
在最终结论前,请声明:我已就[合约代码]的数据,完成了对所有提供指标(第一步中列出的全部项目)的分析,没有遗漏
				""";
		sb.append(command);
		sb.append("\n");
		
		sb.append("数据说明:NA代表当前数据缺失");
		sb.append("\n");
		sb.append("格式说明:不能出现table格式");
		sb.append("\n");
		sb.append("交易时间说明:夜盘21:00-23:00,日盘9:00-10:15,10:30-11:30,13:30-15:00");
		sb.append("\n");
		if(TradeUtil.isUpcomingHoliday()) {
			String nextTradeDate = TradeUtil.getNextTradeDate();
			sb.append("特别说明:下一个交易日为");
			sb.append(nextTradeDate);
			sb.append(",这期间都是公共假日,且交易时间仅为日盘9:00-10:15,10:30-11:30,13:30-15:00");
			sb.append("\n");
		}
		formatList.add(sb);
	}

	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, LocalDateTime current,
			List<Timeseries> realTimeTsList, int limit, String tradeDate) {
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
//			LocalDateTime currentLDT = TradeUtil.getLocalDateTime(ts.getTradeTs());
			if(tradeDate != null) {
				if(tradeDate.equals(ts.getTradeDate())) {
					ret.add(dataSb);
				}
			} else if (freq == FreqEnum._1D || freq == FreqEnum._1W) {
				if (size - i <= 5) {
					ret.add(dataSb);
				} else if (prev != null) {
					int currentDir = ts.getClose().compareTo(prev.getClose());
					if (direction == 0 || currentDir == 0) {
						direction = currentDir;
					} else if (direction * currentDir < 0) {
						direction = currentDir;
						ret.add(prevData);
					}
				} else {
					ret.add(dataSb);
				}
				prev = ts;
				prevData = dataSb;
			} else {
				ret.add(dataSb);
			}
		}
		return ret;
	}

}
