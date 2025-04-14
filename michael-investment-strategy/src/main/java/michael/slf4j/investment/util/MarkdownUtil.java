package michael.slf4j.investment.util;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownUtil {
	public static String convertToHtml(String markdown) {
		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}
}