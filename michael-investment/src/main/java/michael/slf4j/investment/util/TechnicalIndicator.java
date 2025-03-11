package michael.slf4j.investment.util;

import java.util.ArrayList;
import java.util.List;

public class TechnicalIndicator {

	static class StockData {
		public double close;
		public double high;
		public double low;

		public StockData(double close, double high, double low) {
			this.close = close;
			this.high = high;
			this.low = low;
		}
	}

	public static class MACDResult {
		public double dif;
		public double dea;
		public double macd;

		public MACDResult(double dif, double dea, double macd) {
			this.dif = dif;
			this.dea = dea;
			this.macd = macd;
		}
	}

	public static class KDJResult {
		public double k;
		public double d;
		public double j;

		public KDJResult(double k, double d, double j) {
			this.k = k;
			this.d = d;
			this.j = j;
		}
	}

	// 改进的EMA计算（迭代方式）
	private static List<Double> calculateEMAByMACD(List<Double> data, int period) {
		List<Double> ema = new ArrayList<>();
		double multiplier = 2.0 / (period + 1);
		double sum = 0;

		// 计算SMA作为初始值
		for (int i = 0; i < period; i++) {
			sum += data.get(i);
			ema.add(0.0); // 填充无效数据
		}
		ema.set(period - 1, sum / period);

		// 计算后续EMA
		for (int i = period; i < data.size(); i++) {
			double emaValue = data.get(i) * multiplier + ema.get(i - 1) * (1 - multiplier);
			ema.add(emaValue);
		}
		return ema;
	}

	// 修正后的MACD计算
	public static MACDResult calculateMACD(List<Double> closes) {
		List<Double> ema12 = calculateEMAByMACD(closes, 12);
		List<Double> ema26 = calculateEMAByMACD(closes, 26);

		List<Double> difs = new ArrayList<>();
		for (int i = 0; i < closes.size(); i++) {
			if (i < 25) { // 前25个EMA26未完成计算
				difs.add(0.0);
			} else {
				difs.add(ema12.get(i) - ema26.get(i));
			}
		}

		List<Double> dea = calculateEMAByMACD(difs, 9);
		List<MACDResult> results = new ArrayList<>();

		for (int i = 0; i < closes.size(); i++) {
			double macd = 0;
			if (i >= 33) { // 完整数据起始点：25(EMA26)+8(DEA)
				macd = (difs.get(i) - dea.get(i)) * 2;
			}
			results.add(new MACDResult(i >= 25 ? difs.get(i) : 0, i >= 33 ? dea.get(i) : 0, macd));
		}
		return results.get(results.size() - 1);
	}

	// 修正后的KDJ计算
	public static KDJResult calculateKDJ(List<Double> highs, List<Double> lows,
			List<Double> closes) {
		List<KDJResult> results = new ArrayList<>();
		double k = 50.0, d = 50.0;
		final int period = 9;

		for (int i = 0; i < highs.size(); i++) {
			if (i < period - 1) {
				results.add(new KDJResult(50, 50, 50));
				continue;
			}

			// 计算周期内极值
			double highest = highs.get(i);
			double lowest = lows.get(i);
			for (int j = i - period + 1; j <= i; j++) {
				highest = Math.max(highest, highs.get(j));
				lowest = Math.min(lowest, lows.get(j));
			}

			double close = closes.get(i);
			double rsv = 100 * (close - lowest) / (highest - lowest);
			if (Double.isNaN(rsv)) { // 处理极值相等情况
				rsv = i == 0 ? 50 : results.get(i - 1).k;
			}

			// 平滑计算
			k = (2 * k + rsv) / 3;
			d = (2 * d + k) / 3;
			double j = 3 * k - 2 * d;

			results.add(new KDJResult(k, d, j));
		}
		return results.get(results.size() - 1);
	}

}