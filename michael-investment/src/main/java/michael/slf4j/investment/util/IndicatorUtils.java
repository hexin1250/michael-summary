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

	// MACD指标
	public static Map<String, List<Double>> calculateMACD(List<Double> closes) {
        List<Double> ema12 = calculateEMA(closes, 12);
        List<Double> ema26 = calculateEMA(closes, 26);
        
        // 对齐EMA数据（从两者都有效的起始点开始）
        int startIndex = Math.max(ema12.size(), ema26.size()) - Math.min(ema12.size(), ema26.size());
        List<Double> alignedEma12 = ema12.subList(startIndex, ema12.size());
        List<Double> alignedEma26 = ema26.subList(startIndex, ema26.size());
        
        // 计算DIFF
        List<Double> diff = new ArrayList<>();
        for (int i = 0; i < alignedEma26.size(); i++) {
            diff.add(alignedEma12.get(i) - alignedEma26.get(i));
        }
        
        // 计算DEA
        List<Double> dea = calculateEMA(diff, 9);
        
        // 计算MACD柱
        List<Double> macd = new ArrayList<>();
        for (int i = 0; i < dea.size(); i++) {
            int diffIndex = diff.size() - dea.size() + i;
            macd.add(2 * (diff.get(diffIndex) - 2 * dea.get(i)));
        }
        
        // 结果对齐
        int offset = diff.size() - dea.size();
        return Map.of(
            "DIFF", diff.subList(offset, diff.size()),
            "DEA", dea,
            "MACD", macd
        );
    }

    // 精确EMA计算方法（使用SMA初始化）
    private static List<Double> calculateEMA(List<Double> data, int period) {
        List<Double> ema = new ArrayList<>();
        if (data.size() < period) return ema;
        
        // 计算初始SMA
        double sma = data.subList(0, period).stream()
                        .mapToDouble(Double::doubleValue)
                        .average().orElse(0);
        ema.add(sma);
        
        // 计算后续EMA值
        double k = 2.0 / (period + 1);
        for (int i = period; i < data.size(); i++) {
            double emaVal = data.get(i) * k + ema.get(ema.size()-1) * (1 - k);
            ema.add(emaVal);
        }
        return ema;
    }

	// KDJ指标
	public static Map<String, List<Double>> calculateKDJ(List<Double> highs, List<Double> lows, List<Double> closes) {
		int n = 9;
		List<Double> kValues = new ArrayList<>();
		List<Double> dValues = new ArrayList<>();
		List<Double> jValues = new ArrayList<>();

		for (int i = n - 1; i < closes.size(); i++) {
			List<Double> subHigh = highs.subList(i - n + 1, i + 1);
			List<Double> subLow = lows.subList(i - n + 1, i + 1);

			double maxHigh = Collections.max(subHigh);
			double minLow = Collections.min(subLow);
			double rsv = (closes.get(i) - minLow) / (maxHigh - minLow) * 100;

			kValues.add(rsv);
			if (kValues.size() >= 3) {
				double k = (kValues.get(kValues.size() - 1) + kValues.get(kValues.size() - 2) / 2);
				double d = kValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
				double j = 3 * k - 2 * d;
				dValues.add(d);
				jValues.add(j);
			}
		}
		return Map.of("K", kValues, "D", dValues, "J", jValues);
	}

	// 其他指标的实现类似，受篇幅限制这里省略完整实现
	// 需要实现的方法：calculateDMI, calculateBIAS, calculateRSI, calculateWR, calculateATR

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

	// DMI指标 (包含PDI, MDI, ADX, ADXR)
	public static Map<String, List<Double>> calculateDMI(List<Double> highs, List<Double> lows, List<Double> closes,
			int period, int adxrPeriod) {
		int totalDays = highs.size();
		List<Double> trList = new ArrayList<>();
		List<Double> plusDMList = new ArrayList<>();
		List<Double> minusDMList = new ArrayList<>();

		// 计算TR和±DM
		for (int i = 1; i < totalDays; i++) {
			double high = highs.get(i);
			double low = lows.get(i);
			double prevClose = closes.get(i - 1);

			// 真实波幅(TR)
			double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
			trList.add(tr);

			// 方向运动(±DM)
			double upMove = high - highs.get(i - 1);
			double downMove = lows.get(i - 1) - low;
			if (upMove > downMove && upMove > 0) {
				plusDMList.add(upMove);
				minusDMList.add(0.0);
			} else if (downMove > upMove && downMove > 0) {
				minusDMList.add(downMove);
				plusDMList.add(0.0);
			} else {
				plusDMList.add(0.0);
				minusDMList.add(0.0);
			}
		}

		// 平滑处理
		List<Double> plusDI = smooth(plusDMList, trList, period);
		List<Double> minusDI = smooth(minusDMList, trList, period);

		// 计算ADX
		List<Double> dxList = new ArrayList<>();
		for (int i = 0; i < plusDI.size(); i++) {
			double diPlus = plusDI.get(i);
			double diMinus = minusDI.get(i);
			if (diPlus + diMinus == 0) {
				dxList.add(0.0);
			} else {
				dxList.add(100 * Math.abs(diPlus - diMinus) / (diPlus + diMinus));
			}
		}
		List<Double> adx = calculateEMA(dxList, period);

		// 计算ADXR
		List<Double> adxr = new ArrayList<>();
		for (int i = adxrPeriod - 1; i < adx.size(); i++) {
			double sum = 0;
			for (int j = 0; j < adxrPeriod; j++) {
				sum += adx.get(i - j);
			}
			adxr.add(sum / adxrPeriod);
		}

		// 对齐数据长度
		int offset = adx.size() - adxr.size();
		return Map.of("PDI", plusDI.subList(offset, plusDI.size()), "MDI", minusDI.subList(offset, minusDI.size()),
				"ADX", adx.subList(offset, adx.size()), "ADXR", adxr);
	}

	private static List<Double> smooth(List<Double> dmList, List<Double> trList, int period) {
		List<Double> result = new ArrayList<>();
		double sumDM = dmList.subList(0, period).stream().mapToDouble(Double::doubleValue).sum();
		double sumTR = trList.subList(0, period).stream().mapToDouble(Double::doubleValue).sum();
		result.add(100 * sumDM / sumTR);

		for (int i = period; i < dmList.size(); i++) {
			sumDM = sumDM * (period - 1) / period + dmList.get(i);
			sumTR = sumTR * (period - 1) / period + trList.get(i);
			result.add(100 * sumDM / sumTR);
		}
		return result;
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

	// RSI指标
	public static Map<String, List<Double>> calculateRSI(List<Double> closes) {
		return Map.of("RSI1", calculateSingleRSI(closes, 6), "RSI2", calculateSingleRSI(closes, 12), "RSI3",
				calculateSingleRSI(closes, 24));
	}

	private static List<Double> calculateSingleRSI(List<Double> closes, int period) {
		List<Double> gains = new ArrayList<>();
		List<Double> losses = new ArrayList<>();

		for (int i = 1; i < closes.size(); i++) {
			double diff = closes.get(i) - closes.get(i - 1);
			gains.add(diff > 0 ? diff : 0);
			losses.add(diff < 0 ? -diff : 0);
		}

		List<Double> avgGain = calculateMA(gains, period);
		List<Double> avgLoss = calculateMA(losses, period);

		List<Double> rsi = new ArrayList<>();
		for (int i = 0; i < avgGain.size(); i++) {
			double rs = avgLoss.get(i) == 0 ? 100 : avgGain.get(i) / avgLoss.get(i);
			rsi.add(100 - (100 / (1 + rs)));
		}
		return rsi;
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
	
    public static void main(String[] args) {
        // 测试案例1
        List<Double> data1 = Arrays.asList(3287.0, 3685.0, 3690.0, 3685.0, 3687.0, 3684.0, 
            3684.0, 3688.0, 3681.0, 3691.0, 3699.0, 3706.0, 3700.0, 3691.0, 3686.0, 3686.0, 
            3686.0, 3692.0, 3700.0, 3697.0, 3701.0, 3697.0, 3696.0, 3695.0, 3703.0, 3703.0, 
            3703.0, 3704.0, 2702.0, 3703.0, 3701.0, 3701.0, 3687.0, 3689.0, 3700.0, 3691.0, 
            3701.0, 3697.0);
        
        Map<String, List<Double>> result1 = calculateMACD(data1);
        System.out.println("案例1结果：");
        printLastValues(result1, 1);
        
        // 测试案例2
        List<Double> data2 = new ArrayList<>(data1);
        data2.add(3696.0);
        Map<String, List<Double>> result2 = calculateMACD(data2);
        System.out.println("\n案例2结果：");
        printLastValues(result2, 1);
    }
    
    private static void printLastValues(Map<String, List<Double>> result, int count) {
        result.forEach((k, v) -> {
            System.out.printf("%s: %.2f ", k, v.isEmpty() ? 0 : v.get(v.size()-1));
        });
    }
}