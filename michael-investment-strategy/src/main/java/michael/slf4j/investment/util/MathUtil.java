package michael.slf4j.investment.util;

import java.math.BigDecimal;

public class MathUtil {
	public static BigDecimal multiply(BigDecimal a, int b) {
		return a.multiply(new BigDecimal(b));
	}
	
	public static BigDecimal average(BigDecimal a, int b, BigDecimal c, int d) {
		BigDecimal sum = multiply(a, b).add(multiply(c, d));
		return sum.divide(new BigDecimal(b + d));
	}
	
	public static BigDecimal plus(BigDecimal a, BigDecimal b) {
		return a.add(b);
	}

}
