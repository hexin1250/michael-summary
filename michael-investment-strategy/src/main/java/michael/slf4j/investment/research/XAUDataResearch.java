package michael.slf4j.investment.research;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
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
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.DataLoaderUtil;
import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;
import michael.slf4j.investment.util.TradeUtil;

@Component("xauDataResearch")
public class XAUDataResearch {
	private static final Logger log = Logger.getLogger(XAUDataResearch.class);
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
		HEADER_MAP.put("VOLUME", "VOLUME");
		HEADER_MAP.put("MA1", "MA5");
		HEADER_MAP.put("MA2", "MA8");
		HEADER_MAP.put("MA3", "MA13");
		HEADER_MAP.put("MA4", "MA21");
		HEADER_MAP.put("MA5", "MA34");
		HEADER_MAP.put("MA6", "MA55");
		HEADER_MAP.put("MA7", "MA89");
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

	@Value("${chat.history.folder}")
	private String folderName;

	@Autowired
	private FileService fileService;

	private NumberFormat nf;

	public XAUDataResearch() {
		this.nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
	}
	
	public void summarize() {
		Variety variety = Variety.XAUUSD;
		log.info("Start to get new research for " + variety.name());
		LocalDateTime current = LocalDateTime.now();
		Timestamp ts = TradeUtil.getTimestamp(current);
		List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1D.getValue(),
				ts);
		lastTradeDates.remove(0);
		String tTradeDate = lastTradeDates.get(0);
		String mainSecurity = null;
		if(variety == Variety.XAUUSD) {
			mainSecurity = "XAUUSD";
		}

		List<StringBuffer> formatList = new ArrayList<StringBuffer>();

		FreqEnum freq = FreqEnum._1D;
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
		List<Timeseries> realTimeList1D = new ArrayList<>();
		for (Entry<Timestamp, Timeseries> entry : map.entrySet()) {
			realTimeList1D.add(entry.getValue());
		}
		Queue<StringBuffer> queue1D = summarizeDataByFreq(freq, realTimeList1D, 30);
		queue1D.stream().forEach(currentSb -> formatList.add(currentSb));

		/**
		 * 1W frequence data
		 */
		List<Timeseries> realTimeList1W = DataLoaderUtil.generate1WTsListBy1D(realTimeList1D);
		Queue<StringBuffer> queue1W = summarizeDataByFreq(FreqEnum._1W, realTimeList1W, 20);

		queue1D.stream().forEach(currentSb -> formatList.add(currentSb));
		queue1W.stream().forEach(currentSb -> formatList.add(currentSb));

		generateKeyPoints(formatList, realTimeList1D);
		generateTrail(variety.name(), tTradeDate, lastTradeDates.get(1), formatList, mainSecurity, current, realTimeList1D.get(realTimeList1D.size() - 1));

		String fileName = folderName + "/" + variety.name() + "/" + tTradeDate + ".question.txt";
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
		sb.append("现在时间是").append(tDate).append(",");
		sb.append("收盘点位").append(closePrice);
		sb.append("\n");
		sb.append("分析");
		sb.append(mainSecurity);
		sb.append("下一个交易日的日内走势预演,和对应的概率,和关键价位预判\n");
//		Map<String, String> map = PositionFileUtil.readPositionData(varietyStr);
//		StringBuffer anotherCase = new StringBuffer();
//		if (!map.isEmpty()) {
//			int v = Integer.valueOf(map.get(PositionFileUtil.PRICE));
//			int direction = Integer.valueOf(map.get(PositionFileUtil.DIRECTION_INT));
//			anotherCase.append(map.get(PositionFileUtil.DIRECTION));
//			anotherCase.append("开仓价").append(v).append(",");
//			anotherCase.append("浮");
//			if(direction * (closePrice - v) > 0) {
//				anotherCase.append("盈");
//			} else {
//				anotherCase.append("亏");
//			}
//			anotherCase.append(Math.abs(closePrice - v)).append("点,");
//			anotherCase.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%");
//		}
//		sb.append("针对以下情况制定交易策略(");
//		if(anotherCase.isEmpty()) {
//			sb.append("空仓");
//		} else {
//			sb.append(anotherCase);
//		}
//		sb.append(")\n");
		
		String command = """
第一步,请首先确认的所有数据表格和指标清单:
1. 周期指标表:必须包括时间/周期/开盘价/最高价/最低价/收盘价/VOLUME,以及以下所有技术指标:
 - 趋势指标:MA5,MA8,MA13,MA21,MA34,MA55,MA89
 - 通道指标:BOLL LOWER, BOLL MID, BOLL UPPER
 - 震荡/动量指标:BIAS;WR;CCI(14);MFI(14);RSI
 - 趋势振荡指标:MACD
 - 随机指标:KDJ
 - 波动指标:ATR
(请在此条后回复"已确认数据清单"后再进行下一步)
第二步,请确认所有数据清单都将会被用作分析
(请在此条后回复"已确认所有指标都将被分析"后再进行下一步)
第三步:逐项分析
请按以下结构分析,每一项都必须明确引用上一步列出的具体指标名称和最新数值:
1.日线级别顶底分析:基于日线(1D周期)的开盘价/最高价/最低价/收盘价,结合所有形态指标,判断是否存在双顶/双底/头肩等形态,并描述价格相对位置
2.多空争夺点位:确定关键压力和支撑位
3.主力持仓行为解析:推断主力意图
4.日内走势预演:综合以上信息,只分析下一个交易日的盘中波动,给出明天可能的三种走势路径(上涨、震荡、下跌)及其对应的触发条件和概率.并重点分析如果价格突破关键阻力位或跌破关键支撑位后,后续的技术目标位和形态变化.
明确的多空分界定义:分别定义价格有效突破和有效跌破该价位的条件
完整的条件链条:每个方向的定义都必须包含触发条件、确认条件和失效条件
走势推演:在突破或跌破确认后,分别推演后续可能的目标位和形态变化
所有突破、跌破、震荡的判定,必须基于下一个交易日盘中的价格行为,不能以日线收盘作为确认标准
5.关键价位突破或跌破定义:针对你预判中,上涨最重要的一个关键价位和下跌最重要的一个关键价位,请严格按以下框架明确定义(以1小时周期作为评判标准):
触发条件:价格首次收盘/触碰该价位的具体标准(如:1小时周期图收盘价高于或低于XX).
确认条件:在触发后,需要满足哪些条件才能确认为有效突破或有效跌破.
失效条件:在确认后,若后续走势跌破或涨破哪个价位(或满足什么条件),则宣告本次突破或跌破失败,观点需更新为假突破或假跌破.

第四步:交叉验证
在最终结论前,请声明:我已就[合约代码]的数据,完成了对所有提供指标(第一步中列出的全部项目)的分析,没有遗漏
				""";
		sb.append(command);
		sb.append("\n");
		
		sb.append("数据说明:NA代表当前数据缺失");
		sb.append("\n");
		sb.append("格式说明:不能出现table格式");
		sb.append("\n");
		sb.append("交易时间说明:夜盘7:00(T)-6:00(T+1)");
		sb.append("\n");
		formatList.add(sb);
	}

	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, List<Timeseries> realTimeTsList, int limit) {
		List<Double> opens = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();
		List<Double> closes = new ArrayList<Double>();
		List<Double> lows = new ArrayList<Double>();
		List<Double> volumes = new ArrayList<Double>();
		List<StockData> dataList = new ArrayList<>();
		Queue<StringBuffer> ret = new LinkedBlockingQueue<>();

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

			dataMap.put("time", ts.getTradeDate());
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
			ret.add(dataSb);
		}
		return ret;
	}

}
