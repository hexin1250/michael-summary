package michael.slf4j.investment.util;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;

import org.apache.log4j.Logger;

public class WeChatRobot {
	private final static Logger log = Logger.getLogger(WeChatRobot.class);
	
	private Robot bot = null;
	private Clipboard clip = null;

	public WeChatRobot() {
		try {
			this.clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			this.bot = new Robot();
		} catch (AWTException e) {
			log.error("Error during initializing wechat robot", e);
		}
	}
	
	public void sendWechatMessage(String message) {
		OpenWeChat();
		ChooseFriends("Michael小鑫");
		SendMessage(message);
	}

	private void OpenWeChat() {
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_ALT);
		bot.keyPress(KeyEvent.VK_W);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_ALT);
		bot.delay(100);
	}

	private void ChooseFriends(String name) {
		Transferable text = new StringSelection(name);
		clip.setContents(text, null);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_F);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_V);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_ENTER);
		bot.delay(200);
	}

	private void SendMessage(String message) {
		Transferable text = new StringSelection(message);
		clip.setContents(text, null);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_V);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_ENTER);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_ALT);
		bot.keyPress(KeyEvent.VK_W);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_ALT);
	}
}