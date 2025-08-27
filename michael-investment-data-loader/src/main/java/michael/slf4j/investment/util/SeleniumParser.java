package michael.slf4j.investment.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import michael.slf4j.investment.model.FutureSecurityEnum;
import michael.slf4j.investment.model.TopDeal;

public class SeleniumParser implements Closeable {
	
	private final WebDriver driver;
	
	public SeleniumParser() {
		driver = new ChromeDriver();
	}
	
	public List<TopDeal> lookupData(FutureSecurityEnum varietyEnum, String security, String tradeDate) throws InterruptedException {
		driver.get("https://data.eastmoney.com/futures/" + varietyEnum.broker + "/data.html?va=" + varietyEnum.name() + "&ct=" + security);
		List<WebElement> mylist = driver.findElements(By.xpath("/html/body/div/img"));
		mylist.get(mylist.size() - 2).click();
		Thread.sleep(1000);
		List<WebElement> myNextlist = driver.findElements(By.xpath("/html/body/div/img"));
		myNextlist.get(myNextlist.size() - 1).click();
		Thread.sleep(1000);
		
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
		WebElement module = contentPage.findElement(By.className("IFUlModule"));
		
		List<WebElement> tops = new ArrayList<>();
		tops.add(module.findElement(By.className("IFcb1")));
		tops.addAll(module.findElements(By.className("IFcb2")));
		
		List<TopDeal> ret = new ArrayList<>();
		for (WebElement childTop : tops) {
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
					deal.setType(subject.replaceAll("龙虎榜", ""));
					ret.add(deal);
				}
			}
		}
		return ret;
	}

	@Override
	public void close() throws IOException {
		driver.quit();
	}
}