package michael.slf4j.investment.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import io.github.bonigarcia.wdm.WebDriverManager;
import michael.slf4j.investment.listener.NotificationListener;
import michael.slf4j.investment.util.WeChatRobot;

@Component
@Controller
public class InitialRobot implements CommandLineRunner {
//	@Autowired
//	NotificationListener listener;
	
	@Override
	public void run(String... args) throws Exception {
//		WeChatRobot robot = new WeChatRobot();
//		robot.ChooseFriends("Michael小鑫");
		
//		String message = "{\"receiver\":\"Michael小鑫\",\"variety\":\"RB\"}";
//		listener.triggerMessageBus(message);
	}

}
