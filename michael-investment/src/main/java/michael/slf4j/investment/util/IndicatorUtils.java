package michael.slf4j.investment.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndicatorUtils {

	// BBI指标
	public static List<Double> calculateBBI(List<Double> closes) {
		List<Double> ma3 = calculateMA(closes, 3);
		List<Double> ma6 = calculateMA(closes, 6);
		List<Double> ma12 = calculateMA(closes, 12);
		List<Double> ma24 = calculateMA(closes, 24);

		int minLength = Collections.min(Arrays.asList(ma3.size(), ma6.size(), ma12.size(), ma24.size()));
		List<Double> bbi = new ArrayList<>();
		for (int i = 0; i < minLength; i++) {
			int idx = ma3.size() - minLength + i;
			double val = (ma3.get(idx) + ma6.get(idx) + ma12.get(idx) + ma24.get(idx)) / 4;
			bbi.add(val);
		}
		return bbi;
	}

	// BOLL指标
	public static Map<String, List<Double>> calculateBOLL(List<Double> closes, int period, int multiplier) {
		List<Double> mid = calculateMA(closes, period);
		List<Double> upper = new ArrayList<>();
		List<Double> lower = new ArrayList<>();

		for (int i = period - 1; i < closes.size(); i++) {
			List<Double> sub = closes.subList(i - period + 1, i + 1);
			double mean = mid.get(i - period + 1);
			double std = calculateStd(sub, mean);

			upper.add(mean + multiplier * std);
			lower.add(mean - multiplier * std);
		}

		return Map.of("MID", mid, "UPPER", upper, "LOWER", lower);
	}

	// MA指标
	public static Map<String, List<Double>> calculateMA(List<Double> closes) {
		return Map.of("MA5", calculateMA(closes, 5), "MA10", calculateMA(closes, 10), "MA20", calculateMA(closes, 20),
				"MA40", calculateMA(closes, 40), "MA60", calculateMA(closes, 60));
	}

	// 通用MA计算
	private static List<Double> calculateMA(List<Double> data, int period) {
		List<Double> result = new ArrayList<>();
		for (int i = period - 1; i < data.size(); i++) {
			double sum = 0;
			for (int j = i - period + 1; j <= i; j++) {
				sum += data.get(j);
			}
			result.add(sum / period);
		}
		return result;
	}

	// 计算标准差
	private static double calculateStd(List<Double> data, double mean) {
		double variance = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0);
		return Math.sqrt(variance);
	}

	// BIAS指标
	public static Map<String, List<Double>> calculateBIAS(List<Double> closes) {
		List<Double> ma6 = calculateMA(closes, 6);
		List<Double> ma12 = calculateMA(closes, 12);
		List<Double> ma24 = calculateMA(closes, 24);

		List<Double> bias1 = new ArrayList<>();
		List<Double> bias2 = new ArrayList<>();
		List<Double> bias3 = new ArrayList<>();

		for (int i = 0; i < ma6.size(); i++) {
			int closeIndex = closes.size() - ma6.size() + i;
			bias1.add((closes.get(closeIndex) - ma6.get(i)) / ma6.get(i) * 100);
		}
		for (int i = 0; i < ma12.size(); i++) {
			int closeIndex = closes.size() - ma12.size() + i;
			bias2.add((closes.get(closeIndex) - ma12.get(i)) / ma12.get(i) * 100);
		}
		for (int i = 0; i < ma24.size(); i++) {
			int closeIndex = closes.size() - ma24.size() + i;
			bias3.add((closes.get(closeIndex) - ma24.get(i)) / ma24.get(i) * 100);
		}

		return Map.of("BIAS1", bias1, "BIAS2", bias2, "BIAS3", bias3);
	}

	// WR指标
	public static Map<String, List<Double>> calculateWR(List<Double> highs, List<Double> lows, List<Double> closes) {
		List<Double> wr1 = calculateSingleWR(highs, lows, closes, 10);
		List<Double> wr2 = calculateSingleWR(highs, lows, closes, 6);
		return Map.of("WR1", wr1, "WR2", wr2);
	}

	private static List<Double> calculateSingleWR(List<Double> highs, List<Double> lows, List<Double> closes,
			int period) {
		List<Double> wrValues = new ArrayList<>();
		for (int i = period - 1; i < closes.size(); i++) {
			List<Double> subHigh = highs.subList(i - period + 1, i + 1);
			List<Double> subLow = lows.subList(i - period + 1, i + 1);

			double highest = Collections.max(subHigh);
			double lowest = Collections.min(subLow);
			double wr = (highest - closes.get(i)) / (highest - lowest) * 100;
			wrValues.add(wr);
		}
		return wrValues;
	}

	// ATR指标
	public static Map<String, List<Double>> calculateATR(List<Double> highs, List<Double> lows, List<Double> closes,
			int period) {
		List<Double> trValues = new ArrayList<>();
		for (int i = 1; i < highs.size(); i++) {
			double tr = Math.max(highs.get(i) - lows.get(i),
					Math.max(Math.abs(highs.get(i) - closes.get(i - 1)), Math.abs(lows.get(i) - closes.get(i - 1))));
			trValues.add(tr);
		}

		List<Double> atr = calculateMA(trValues, period);
		return Map.of("TR", trValues.subList(period - 1, trValues.size()), "ATR", atr);
	}

	public static List<Double> calculateCCI(List<Double> highs, List<Double> lows, List<Double> closes, int period) {
		List<Double> cciList = new ArrayList<>();
		for (int i = period - 1; i < closes.size(); i++) {
			List<Double> typicalPrices = new ArrayList<>();
			for (int j = i - period + 1; j <= i; j++) {
				double tp = (highs.get(j) + lows.get(j) + closes.get(j)) / 3;
				typicalPrices.add(tp);
			}
			double sma = typicalPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
			double meanDeviation = typicalPrices.stream().mapToDouble(tp -> Math.abs(tp - sma)).average().orElse(0);
			double currentTP = (highs.get(i) + lows.get(i) + closes.get(i)) / 3;
			double cci = (currentTP - sma) / (0.015 * meanDeviation);
			cciList.add(cci);
		}
		return cciList;
	}

	// 三参数ENE指标（周期10，上轨+11%，下轨-9%）
	public static Map<String, List<Double>> calculateENE_10_11_9(List<Double> closes) {
		List<Double> mid = calculateMA(closes, 10);
		List<Double> upper = new ArrayList<>();
		List<Double> lower = new ArrayList<>();
		List<Double> ene = new ArrayList<>();

		for (Double m : mid) {
			double upperValue = m * 1.11D;
			double lowerValue = m * 0.91D;
			ene.add((upperValue + lowerValue) / 2);
			upper.add(upperValue); // 上轨+11%
			lower.add(lowerValue); // 下轨-9%
		}

		return Map.of("UPPER", upper, "LOWER", lower, "ENE", ene);
	}

}