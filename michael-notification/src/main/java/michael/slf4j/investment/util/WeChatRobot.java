package michael.slf4j.investment.util;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
//		ChooseFriends("Michael小鑫");
		SendMessage(message);
	}

	public void OpenWeChat() {
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_ALT);
		bot.keyPress(KeyEvent.VK_W);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_ALT);
		bot.keyRelease(KeyEvent.VK_W);
		bot.delay(100);
	}

	public void ChooseFriends(String name) {
		OpenWeChat();
		StringSelection text = new StringSelection(name);
		clip.setContents(text, null);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_F);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_F);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_V);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_V);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_ENTER);
		bot.keyRelease(KeyEvent.VK_ENTER);
		bot.delay(200);
		OpenWeChat();
	}

	private void SendMessage(String message) {
		StringSelection text = new StringSelection(message);
		clip.setContents(text, null);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_V);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_V);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_ENTER);
		bot.keyRelease(KeyEvent.VK_ENTER);
		bot.delay(500);
		bot.keyPress(KeyEvent.VK_CONTROL);
		bot.keyPress(KeyEvent.VK_ALT);
		bot.keyPress(KeyEvent.VK_W);
		bot.keyRelease(KeyEvent.VK_CONTROL);
		bot.keyRelease(KeyEvent.VK_ALT);
		bot.keyRelease(KeyEvent.VK_W);
		bot.delay(200);
	}
	
	public void sendWeChatImage() {
		OpenWeChat();
        pasteAndSendImage();         // 粘贴图片并发送
    }

    private void pasteAndSendImage() {
        bot.delay(300);                   // 等剪贴板更新

        // Ctrl+V 粘贴
        bot.keyPress(KeyEvent.VK_CONTROL);
        bot.keyPress(KeyEvent.VK_V);
        bot.keyRelease(KeyEvent.VK_V);
        bot.keyRelease(KeyEvent.VK_CONTROL);
        bot.delay(1000);                  // 等图片在输入框渲染完成，时间可根据网速/图片大小调整

        // 回车发送
        bot.keyPress(KeyEvent.VK_ENTER);
        bot.keyRelease(KeyEvent.VK_ENTER);
        bot.delay(800);                   // 等待发送完成

        // 可选：发送后关闭微信窗口（与 SendMessage 保持一致）
        bot.keyPress(KeyEvent.VK_CONTROL);
        bot.keyPress(KeyEvent.VK_ALT);
        bot.keyPress(KeyEvent.VK_W);
        bot.keyRelease(KeyEvent.VK_CONTROL);
        bot.keyRelease(KeyEvent.VK_ALT);
        bot.keyRelease(KeyEvent.VK_W);
        bot.delay(200);
    }

}