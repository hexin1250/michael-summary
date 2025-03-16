package michael.slf4j.investment.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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

		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("BOLL LOWER", lower);
		map.put("BOLL MID", mid);
		map.put("BOLL UPPER", upper);
		return map;
	}

	// MA指标
	public static Map<String, List<Double>> calculateMA(List<Double> closes) {
		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("MA1", calculateMA(closes, 5));
		map.put("MA2", calculateMA(closes, 10));
		map.put("MA3", calculateMA(closes, 20));
		map.put("MA4", calculateMA(closes, 40));
		map.put("MA5", calculateMA(closes, 60));
		return map;
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

		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("BIAS1", bias1);
		map.put("BIAS2", bias2);
		map.put("BIAS3", bias3);
		return map;
	}

	// WR指标
	public static Map<String, List<Double>> calculateWR(List<Double> highs, List<Double> lows, List<Double> closes) {
		List<Double> wr1 = calculateSingleWR(highs, lows, closes, 10);
		List<Double> wr2 = calculateSingleWR(highs, lows, closes, 6);
		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("WR1", wr1);
		map.put("WR2", wr2);
		return map;
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
		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("TR", trValues.subList(period - 1, trValues.size()));
		map.put("ATR", atr);
		return map;
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
		
		Map<String, List<Double>> map = new LinkedHashMap<String, List<Double>>();
		map.put("ENE LOWER", lower);
		map.put("ENE ENE", ene);
		map.put("ENE UPPER", upper);
		return map;
	}
	
	/**
     * 计算指数移动平均线（EMA）
     * 
     * @param closes   包含价格数据的数组
     * @param period EMA周期（例如：12日EMA）
     * @return EMA数组，长度与输入数据相同。前(period-1)个位置为0，后续为有效EMA值
     * @throws IllegalArgumentException 如果输入数据不合法
     */
    public static double calculateEMA(List<Double> closes, int period) {
        if (closes == null) {
            throw new IllegalArgumentException("数据数组不能为null");
        }
        if (period < 1) {
            throw new IllegalArgumentException("周期必须大于等于1");
        }
        int size = closes.size();
        if (size < period) {
            throw new IllegalArgumentException("数据数组长度必须大于等于周期");
        }

        double[] ema = new double[size];
        double sma = 0.0;

        // 计算初始SMA（简单移动平均）
        for (int i = 0; i < period; i++) {
            sma += closes.get(i);
        }
        sma /= period;
        ema[period - 1] = sma;

        double alpha = 2.0 / (period + 1); // 平滑系数

        // 计算后续EMA值
        for (int i = period; i < size; i++) {
            ema[i] = closes.get(i) * alpha + ema[i - 1] * (1 - alpha);
        }

        return ema[size - 1];
    }
    
    public static double calculateMFI(List<Double> highs, List<Double> lows, List<Double> closes, List<Double> volumes, int period) {
        List<Double> mfiValues = new ArrayList<>();

        // 验证输入数据有效性
        int dataLength = highs.size();
        if (dataLength != lows.size() || dataLength != closes.size() || dataLength != volumes.size()) {
            throw new IllegalArgumentException("输入数组长度不一致");
        }
        if (dataLength < period + 1) {
            return -10000D; // 数据不足时返回空列表
        }

        // 计算典型价格和原始资金流
        double[] typicalPrices = new double[dataLength];
        double[] rawMoneyFlow = new double[dataLength];
        for (int i = 0; i < dataLength; i++) {
            typicalPrices[i] = (highs.get(i) + lows.get(i) + closes.get(i)) / 3.0;
            rawMoneyFlow[i] = typicalPrices[i] * volumes.get(i);
        }

        // 计算正向和负向资金流
        double[] positiveFlow = new double[dataLength - 1];
        double[] negativeFlow = new double[dataLength - 1];
        for (int i = 1; i < dataLength; i++) {
            if (typicalPrices[i] > typicalPrices[i - 1]) {
                positiveFlow[i - 1] = rawMoneyFlow[i];
                negativeFlow[i - 1] = 0;
            } else if (typicalPrices[i] < typicalPrices[i - 1]) {
                negativeFlow[i - 1] = rawMoneyFlow[i];
                positiveFlow[i - 1] = 0;
            } else {
                positiveFlow[i - 1] = 0;
                negativeFlow[i - 1] = 0;
            }
        }

        // 计算MFI值
        for (int i = period - 1; i < positiveFlow.length; i++) {
            double sumPositive = 0;
            double sumNegative = 0;

            // 累加周期内的资金流
            for (int j = 0; j < period; j++) {
                int index = i - period + 1 + j;
                sumPositive += positiveFlow[index];
                sumNegative += negativeFlow[index];
            }

            // 处理除零情况并计算MFI
            if (sumNegative == 0) {
                mfiValues.add(100.0);
            } else {
                double ratio = sumPositive / sumNegative;
                double mfi = 100.0 - (100.0 / (1 + ratio));
                mfiValues.add(mfi);
            }
        }

        return mfiValues.get(mfiValues.size() - 1);
    }

}