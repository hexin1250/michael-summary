package michael.slf4j.investment.listener;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.util.WeChatRobot;

@Component
public class NotificationListener implements MessageListener {
	private static final Logger log = Logger.getLogger(NotificationListener.class);
	
	@Value(value = "${notification.activemq.topic}")
	private String topic;
	
	@Autowired
	private WeChatRobot robot;
	
	@Override
	@JmsListener(destination = "${notification.activemq.topic}")
	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage) message;
			String notification = textMessage.getText();
			robot.sendWechatMessage(notification);
		} catch (Exception e) {
			log.error("Error during receiving message from the topic:" + topic, e);
		}
	}
}