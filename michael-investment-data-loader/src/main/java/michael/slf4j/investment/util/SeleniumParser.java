package michael.slf4j.investment.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import michael.slf4j.investment.model.FutureSecurityEnum;
import michael.slf4j.investment.model.TopDeal;

public class SeleniumParser implements Closeable {
	private static final Logger log = Logger.getLogger(SeleniumParser.class);
	
	private final WebDriver driver;
	
	public SeleniumParser() {
		driver = new ChromeDriver();
	}
	
	public List<TopDeal> lookupData(FutureSecurityEnum varietyEnum, String security, String tradeDate) throws InterruptedException {
		String url = "https://data.eastmoney.com/futures/" + varietyEnum.broker + "/data.html?va=" + varietyEnum.name() + "&ct=" + security;
		log.info("Trying to access: " + url);
		driver.get(url);
//		List<WebElement> mylist = driver.findElements(By.xpath("/html/body/div/img"));
//		log.info(mylist);
//		mylist.get(mylist.size() - 2).click();
//		Thread.sleep(1000);
//		List<WebElement> myNextlist = driver.findElements(By.xpath("/html/body/div/img"));
//		myNextlist.get(myNextlist.size() - 1).click();
//		Thread.sleep(1000);
		
		WebElement mainBox = driver.findElement(By.className("main-content"));
		WebElement frameContent = mainBox.findElement(By.className("framecontent"));
		WebElement titleWrap = frameContent.findElement(By.className("title-wrap-auto"));
		WebElement futurePage = titleWrap.findElement(By.className("page_futures"));
		WebElement rightPage = futurePage.findElement(By.className("right_cont"));

		WebElement inputBox = rightPage.findElement(By.id("inputDate"));
		if(tradeDate == null) {
			tradeDate = inputBox.getText();
		}
		
		WebElement contentPage = rightPage.findElement(By.className("content"));
		List<TopDeal> ret = new ArrayList<>();
		loadData(ret, contentPage, varietyEnum, security, tradeDate);
		return ret;
	}

	private void loadData(List<TopDeal> ret, WebElement contentPage, FutureSecurityEnum varietyEnum, String security, String tradeDate) {
		List<WebElement> modules = contentPage.findElements(By.className("IFUlModule"));
		for (int i = 1; i <= 2; i++) {
			WebElement module = modules.get(i);
			WebElement childTop = module.findElement(By.className("IFcb1"));
			
			String subject = childTop.findElement(By.className("IFtit")).getText();
			WebElement table = childTop.findElement(By.className("IFUlDiv"));
			List<WebElement> dataList = table.findElements(By.tagName("ul"));
			for (WebElement data : dataList) {
				String id = data.getAttribute("id");
				if(id.isBlank()) {
					continue;
				}
				List<WebElement> recordList = data.findElements(By.tagName("li"));
				for (WebElement record : recordList) {
					String attr = record.getAttribute("data");
					if(attr == null || attr.isBlank()) {
						continue;
					}
					List<WebElement> spanList = record.findElements(By.tagName("span"));
					
					TopDeal deal = new TopDeal();
					deal.setVariety(varietyEnum.name());
					deal.setSecurity(security);
					deal.setTradeDate(tradeDate);
					deal.setTop(Integer.valueOf(spanList.get(1).getText()));
					deal.setClient(spanList.get(2).getText());
					deal.setVolume(Integer.valueOf(spanList.get(3).getText()));
					deal.setOffset(Integer.valueOf(spanList.get(4).getText()));
					deal.setType(subject);
					ret.add(deal);
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		driver.quit();
	}
}