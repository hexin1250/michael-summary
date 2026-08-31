package michael.slf4j.investment.listener;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import michael.slf4j.investment.clipboard.FullPageToClipboard;
import michael.slf4j.investment.util.WeChatRobot;

@Service
public class NotificationListener implements MessageListener {
	private static final Logger log = Logger.getLogger(NotificationListener.class);
	
	@Value(value = "${notification.activemq.topic}")
	private String topic;
	
	@Autowired
	private WeChatRobot robot;
	
	@Autowired
	private FullPageToClipboard pageClip;
	
	@Override
	@JmsListener(destination = "${notification.activemq.topic}")
	public void onMessage(Message message) {
		String notification = null;
		try {
			TextMessage textMessage = (TextMessage) message;
			notification = textMessage.getText();
		} catch (Exception e) {
			log.error("Error during receiving message from the topic:" + topic, e);
			return;
		}
		triggerMessageBus(notification);
	}

	public void triggerMessageBus(String notification) {
		String variety = null;
		StringBuffer sb = new StringBuffer();
		sb.append("http://localhost:1702/apps/strategy/realtime?variety=");
		try {
			JSONObject json = new JSONObject(notification);
			String friend = json.getString("receiver");
			variety = json.getString("variety");
			sb.append(variety);
			robot.ChooseFriends(friend);
			pageClip.generateImage(sb.toString());
			robot.sendWeChatImage();
		} catch (Exception e) {
			log.error("Error", e);
			robot.ChooseFriends("Michael小鑫");
			robot.sendWechatMessage(notification);
		}
	}
	
	public static String[] splitNotification(String notification) {
        if (notification == null || notification.isEmpty()) {
            return new String[0];
        }

        // 按换行符分割，保留空行标志（但后续会过滤掉空行）
        String[] lines = notification.split("\\R", -1);
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            // 忽略空行（即长度为0的行）
            if (!line.isEmpty()) {
                nonEmptyLines.add(line);
            }
        }

        if (nonEmptyLines.isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        int maxLen = 1900;
        int startIdx = 0;

        while (startIdx < nonEmptyLines.size()) {
            // 当前段包含的第一个行索引
            int currentLineIdx = startIdx;
            // 当前段构建的字符串（使用 StringBuilder 方便计算长度）
            StringBuilder segment = new StringBuilder(nonEmptyLines.get(currentLineIdx));
            currentLineIdx++;

            // 尝试加入更多行，直到超长
            while (currentLineIdx < nonEmptyLines.size()) {
                String nextLine = nonEmptyLines.get(currentLineIdx);
                // 加上换行符和下一行的长度
                int addedLen = 1 + nextLine.length();  // 1 代表换行符 '\n'
                if (segment.length() + addedLen <= maxLen) {
                    segment.append('\n').append(nextLine);
                    currentLineIdx++;
                } else {
                    break;
                }
            }
            result.add(segment.toString());
            startIdx = currentLineIdx;
        }

        return result.toArray(new String[0]);
    }

}