package michael.slf4j.investment.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PositionFileUtil {
	private static final String POSITION_FILE_NAME = "C:/Users/HP/python-workspace/myproject/data/position.properties";
	
	public static final String DIRECTION = "direction";
	public static final String PRICE = "price";
	public static final String POSITION_PER = "positionPer";
	
	public static String savePositionData(int direction, int price, int positionPer) {
        Properties prop = new Properties();
        prop.setProperty(DIRECTION, direction + "");
        prop.setProperty(PRICE, price + "");
        prop.setProperty(POSITION_PER, positionPer + "");

        try (OutputStream fos = new FileOutputStream(POSITION_FILE_NAME)) {
            // 保存到文件（注释会以#开头）
            prop.store(fos, "Position Information");
            fos.flush();
        } catch (IOException e) {
        	return e.getMessage();
        }
		return "保存成功";
	}
	
	public static Map<String, String> readPositionData() {
		Properties prop = new Properties();
		Map<String, String> ret = new HashMap<>();
        try (InputStream fis = new FileInputStream(POSITION_FILE_NAME)) {
            prop.load(fis);
            int direction = Integer.valueOf(prop.getProperty(DIRECTION));
            if(direction == 0) {
            	return ret;
            }
            ret.put(DIRECTION, direction == 1 ? "多单" : "空单");
            ret.put(PRICE, prop.getProperty(PRICE));
            ret.put(POSITION_PER, prop.getProperty(POSITION_PER));
            return ret;
        } catch (Exception e) {
        	return ret;
        }
	}
	
	public static StringBuffer getDeepseek() {
		String fileName = "C:/Users/HP/python-workspace/myproject/data/reason_output.txt";
		File file = new File(fileName);
        long timestamp = file.lastModified(); // 获取时间戳（毫秒）
        Date date = new Date(timestamp);
		StringBuffer sb = new StringBuffer();
		sb.append("<br>").append(date);
		try {
			List<String> list = getAllLines(fileName);
			list.stream().forEach(line -> {
				sb.append("<br>");
				sb.append(line);
			});
		} catch (IOException e) {
			e.printStackTrace();
			sb.append("Issue happened:").append(e.getMessage());
		}
		return sb;
	}
	
	public static List<String> getAllLines(String fileName) throws IOException{
		return Files.readAllLines(new File(fileName).toPath());
	}

}
