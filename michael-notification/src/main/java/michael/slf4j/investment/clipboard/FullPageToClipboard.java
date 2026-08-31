package michael.slf4j.investment.clipboard;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class FullPageToClipboard {
	
	public FullPageToClipboard() {
		WebDriverManager.chromedriver().setup();
	}

    public void generateImage(String url) throws Exception {
        // 1. 配置 Chrome
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*"); // 避免 WebSocket 连接错误
        // 如需无头模式，取消下面注释
        // options.addArguments("--headless=new");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize(); // 最大化以获取稳定视口
        driver.get(url); // 替换为你的目标网址

        // 2. 等待页面稳定（可根据实际情况替换为显式等待）
        Thread.sleep(3000);

        // 3. 临时隐藏固定元素（如导航栏、浮动按钮等）
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // 替换选择器为实际存在的固定元素，如果没有可注释掉
        try {
            js.executeScript("document.querySelector('header').style.display='none';");
            js.executeScript("document.querySelector('.fixed-banner').style.display='none';");
        } catch (Exception ignored) {}

        // 4. 获取页面尺寸
        long pageHeight = (long) js.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);");
        long viewportHeight = (long) js.executeScript("return window.innerHeight");

        // 5. 分段截图
        int scrollStep = (int) viewportHeight;
        List<BufferedImage> imageParts = new ArrayList<>();

        for (int scrollY = 0; scrollY < pageHeight; scrollY += scrollStep) {
            js.executeScript("window.scrollTo(0, " + scrollY + ");");
            // 等待渲染，重要！可根据页面复杂度调整时长
            Thread.sleep(1000);

            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage image = ImageIO.read(screenshotFile);
            imageParts.add(image);
        }

        // 6. 拼接成完整长图
        BufferedImage fullImage = stitchImages(imageParts);

        // 7. 放入系统剪贴板
        setClipboardImage(fullImage);

        // 8. 清理资源
        driver.quit();
        System.out.println("全页截图已复制到剪贴板，可直接粘贴。");
    }

    /** 垂直拼接多张等宽图片 */
    private static BufferedImage stitchImages(List<BufferedImage> images) {
        int totalHeight = 0;
        int maxWidth = 0;
        for (BufferedImage img : images) {
            totalHeight += img.getHeight();
            maxWidth = Math.max(maxWidth, img.getWidth());
        }

        BufferedImage result = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = result.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, maxWidth, totalHeight);   // 填充白色背景
        int currentY = 0;
        for (BufferedImage img : images) {
            g.drawImage(img, 0, currentY, null);
            currentY += img.getHeight();
        }
        g.dispose();
        return result;
    }

    /** 将 BufferedImage 写入系统剪贴板 */
    private static void setClipboardImage(BufferedImage image) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        TransferableImage transferable = new TransferableImage(image);
        clipboard.setContents(transferable, null);
    }

    /** 实现 Transferable 接口，告知系统数据是图片 */
    private record TransferableImage(BufferedImage image) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return image;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}