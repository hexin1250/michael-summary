package michael.slf4j.investment.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MyFileUtil {
	public static <T> void writeFile(String fileName, T obj) throws FileNotFoundException, IOException {
		try(ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fileName))){
			os.writeObject(obj);
			os.flush();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T readObject(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))){
			return (T) ois.readObject();
		}
	}

}
