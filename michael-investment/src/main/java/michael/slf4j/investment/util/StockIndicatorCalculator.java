package michael.slf4j.investment.util;

import java.util.ArrayList;
import java.util.List;

public class StockIndicatorCalculator {

	public static class StockData {
		private final double open;
		private final double high;
		private final double low;
		private final double close;
		private final double volume;
		private final double prevClose; // 前一日收盘价

		public StockData(double open, double high, double low, double close, double volume, double prevClose) {
			this.open = open;
			this.high = high;
			this.low = low;
			this.close = close;
			this.volume = volume;
			this.prevClose = prevClose;
		}

		// Getters
		public double getHigh() {
			return high;
		}

		public double getLow() {
			return low;
		}

		public double getClose() {
			return close;
		}

		public double getOpen() {
			return open;
		}

		public double getVolume() {
			return volume;
		}

		public double getPrevClose() {
			return prevClose;
		}
	}

	public static class DMIResult {
		private final Double pdi;
		private final Double mdi;
		private final Double adx;

		public DMIResult(Double pdi, Double mdi, Double adx) {
			this.pdi = pdi;
			this.mdi = mdi;
			this.adx = adx;
		}

		// Getters
		public Double getPdi() {
			return pdi;
		}

		public Double getMdi() {
			return mdi;
		}

		public Double getAdx() {
			return adx;
		}
	}

	public static class RSIResult {
		private final Double rsi6;
		private final Double rsi12;
		private final Double rsi24;

		public RSIResult(Double rsi6, Double rsi12, Double rsi24) {
			this.rsi6 = rsi6;
			this.rsi12 = rsi12;
			this.rsi24 = rsi24;
		}

		// Getters
		public Double getRsi6() {
			return rsi6;
		}

		public Double getRsi12() {
			return rsi12;
		}

		public Double getRsi24() {
			return rsi24;
		}
	}

	// DMI计算（14,6）
	public static DMIResult calculateDMI(List<StockData> data) {
		final int diPeriod = 14;
		final int adxPeriod = 6;
		List<DMIResult> results = new ArrayList<>();

		List<Double> trList = new ArrayList<>();
		List<Double> plusDMList = new ArrayList<>();
		List<Double> minusDMList = new ArrayList<>();

		// 计算TR、+DM、-DM
		for (int i = 0; i < data.size(); i++) {
			StockData d = data.get(i);
			if (i == 0) {
				trList.add(d.getHigh() - d.getLow());
				plusDMList.add(0.0);
				minusDMList.add(0.0);
				continue;
			}

			double tr = Math.max(d.getHigh() - d.getLow(),
					Math.max(Math.abs(d.getHigh() - d.getPrevClose()), Math.abs(d.getLow() - d.getPrevClose())));
			trList.add(tr);

			double plusDM = d.getHigh() - data.get(i - 1).getHigh();
			double minusDM = data.get(i - 1).getLow() - d.getLow();
			plusDM = Math.max(plusDM, 0);
			minusDM = Math.max(minusDM, 0);

			if (plusDM > minusDM) {
				minusDM = 0.0;
			} else if (plusDM < minusDM) {
				plusDM = 0.0;
			} else {
				plusDM = 0.0;
				minusDM = 0.0;
			}
			plusDMList.add(plusDM);
			minusDMList.add(minusDM);
		}

		// 平滑计算（Wilder方法）
		List<Double> smoothedTR = smoothValues(trList, diPeriod);
		List<Double> smoothedPlusDM = smoothValues(plusDMList, diPeriod);
		List<Double> smoothedMinusDM = smoothValues(minusDMList, diPeriod);

		// 计算±DI
		List<Double> plusDI = new ArrayList<>();
		List<Double> minusDI = new ArrayList<>();
		for (int i = 0; i < smoothedTR.size(); i++) {
			double tr = smoothedTR.get(i);
			double pdi = (smoothedPlusDM.get(i) / tr) * 100;
			double mdi = (smoothedMinusDM.get(i) / tr) * 100;
			plusDI.add(pdi);
			minusDI.add(mdi);
		}

		// 计算ADX
		List<Double> dxValues = new ArrayList<>();
		for (int i = 0; i < plusDI.size(); i++) {
			double diDiff = Math.abs(plusDI.get(i) - minusDI.get(i));
			double diSum = plusDI.get(i) + minusDI.get(i);
			dxValues.add(diSum != 0 ? (diDiff / diSum) * 100 : 0.0);
		}
		List<Double> adxValues = smoothValues(dxValues, adxPeriod);

		// 组装结果
		int totalDays = data.size();
		int offset = diPeriod + adxPeriod - 2;
		for (int i = 0; i < totalDays; i++) {
			Double pdi = (i >= diPeriod - 1 && i - diPeriod + 1 < plusDI.size()) ? plusDI.get(i - diPeriod + 1) : null;
			Double mdi = (i >= diPeriod - 1 && i - diPeriod + 1 < minusDI.size()) ? minusDI.get(i - diPeriod + 1)
					: null;
			Double adx = (i >= offset && i - offset < adxValues.size()) ? adxValues.get(i - offset) : null;

			results.add(new DMIResult(pdi, mdi, adx));
		}

		return results.get(results.size() - 1);
	}

	// RSI计算（6,12,24）
	public static RSIResult calculateRSI(List<StockData> data) {
		int[] periods = { 6, 12, 24 };
		List<RSIResult> results = new ArrayList<>();

		List<List<Double>> allRSI = new ArrayList<>();
		for (int period : periods) {
			allRSI.add(calculateSingleRSI(data, period));
		}

		for (int i = 0; i < data.size(); i++) {
			results.add(new RSIResult(allRSI.get(0).get(i), allRSI.get(1).get(i), allRSI.get(2).get(i)));
		}

		return results.get(results.size() - 1);
	}

	private static List<Double> calculateSingleRSI(List<StockData> data, int period) {
		List<Double> rsi = new ArrayList<>(data.size());
		List<Double> gains = new ArrayList<>();
		List<Double> losses = new ArrayList<>();

		// 计算价格变化
		for (int i = 0; i < data.size(); i++) {
			if (i == 0) {
				gains.add(0.0);
				losses.add(0.0);
				continue;
			}
			double change = data.get(i).getClose() - data.get(i - 1).getClose();
			gains.add(Math.max(change, 0.0));
			losses.add(Math.max(-change, 0.0));
		}

		// 计算初始平均值
		double avgGain = 0, avgLoss = 0;
		for (int i = 1; i <= period; i++) {
			avgGain += gains.get(i);
			avgLoss += losses.get(i);
		}
		avgGain /= period;
		avgLoss /= period;

		// 计算RSI
		for (int i = 0; i < data.size(); i++) {
			if (i <= period) {
				rsi.add(null);
				continue;
			}

			avgGain = (avgGain * (period - 1) + gains.get(i)) / period;
			avgLoss = (avgLoss * (period - 1) + losses.get(i)) / period;

			double rs = avgLoss != 0 ? avgGain / avgLoss : Double.POSITIVE_INFINITY;
			double currentRSI = 100 - (100 / (1 + rs));
			rsi.add(currentRSI);
		}

		return rsi;
	}

	// Wilder平滑方法
	private static List<Double> smoothValues(List<Double> values, int period) {
		List<Double> smoothed = new ArrayList<>();
		if (values.size() < period)
			return smoothed;

		// 初始值：前period天的平均值
		double sum = 0;
		for (int i = 0; i < period; i++) {
			sum += values.get(i);
		}
		smoothed.add(sum / period);

		// 后续值：平滑计算
		for (int i = period; i < values.size(); i++) {
			double prev = smoothed.get(smoothed.size() - 1);
			double current = (prev * (period - 1) + values.get(i)) / period;
			smoothed.add(current);
		}

		return smoothed;
	}

}