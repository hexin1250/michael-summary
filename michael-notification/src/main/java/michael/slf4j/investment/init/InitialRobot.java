package michael.slf4j.investment.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.util.WeChatRobot;

@Component
@Controller
public class InitialRobot implements CommandLineRunner {
	
	@Override
	public void run(String... args) throws Exception {
		WeChatRobot robot = new WeChatRobot();
		robot.OpenWeChat();
		robot.ChooseFriends("Michael小鑫");
		robot.OpenWeChat();
	}

}
