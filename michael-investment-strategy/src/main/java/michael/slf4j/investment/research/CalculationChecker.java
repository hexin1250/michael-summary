package michael.slf4j.investment.research;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;

public class CalculationChecker {
	private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();

	static {
		HEADER_MAP.put("time", "时间");
		HEADER_MAP.put("open", "开盘价");
		HEADER_MAP.put("high", "最高价");
		HEADER_MAP.put("low", "最低价");
		HEADER_MAP.put("close", "收盘价");
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

	public static void main(String[] args) throws FileNotFoundException, IOException {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
		
		List<Double> opens = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();
		List<Double> closes = new ArrayList<Double>();
		List<Double> lows = new ArrayList<Double>();
		List<Double> volumes = new ArrayList<Double>();
		List<StockData> dataList = new ArrayList<>();
		Queue<StringBuffer> ret = new LinkedBlockingQueue<>();
		
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("src/test/data/000905.txt")))){
			String line = null;
			while((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				String time = parts[0];
				double open = Double.valueOf(parts[1]);
				double high = Double.valueOf(parts[2]);
				double low = Double.valueOf(parts[3]);
				double close = Double.valueOf(parts[4]);
				double volume = Double.valueOf(parts[5]);
				
				opens.add(open);
				highs.add(high);
				lows.add(low);
				closes.add(close);
				volumes.add(volume);
				double preClose = 0D;
				if (!dataList.isEmpty()) {
					preClose = dataList.get(dataList.size() - 1).getClose();
				}
				dataList.add(new StockData(open, high, low, close, volume, preClose));
				
				Map<String, String> dataMap = new LinkedHashMap<String, String>();
				dataMap.put("time", time);
				dataMap.put("open", nf.format(open));
				dataMap.put("high", nf.format(high));
				dataMap.put("low", nf.format(low));
				dataMap.put("close", nf.format(close));
				dataMap.put("VOLUME", nf.format(volume));
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
				
				ret.add(dataSb);
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("下面表格包括了月线指标的数据:");
			sb.append("\n");
			sb.append("|");
			sb.append(HEADER_MAP.values().stream().collect(Collectors.joining("|")));
			sb.append("|");
			sb.append("\n");
			
			System.out.print(sb);
			ret.stream().forEach(dataSb -> System.out.print(dataSb));
			
			String tail = """
这是中证500的月k线数据,根据所有指标,预测接下来的1个月/2个月/3个月,分别跌破5%和涨破15%的概率分别是多少(基于最后一个数据的收盘价计算涨跌幅)
数据说明:NA代表当前数据缺失
格式说明:不能出现table格式
			""";
			System.out.println(tail);
		}
	}

}
