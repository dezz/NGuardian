package net.dezz.nguardian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;

public class Configuration {
	private Properties properties;
	private static Logger logger = Logger.getLogger(Configuration.class);
	
	public Configuration(String filename) {
		FileInputStream fis = null;
		InputStreamReader isr = null;
		Properties prop = new Properties();
		
		try {
			fis = new FileInputStream(filename);
			isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
			prop.load(isr);
			
		} catch (FileNotFoundException e) {
			logger.error("Failed to load properties.", e);
			
		} catch (IOException e) {
			logger.error("Failed to load properties.", e);
			
		} finally {
			if (fis != null) {
				CloseableUtility.safeClose(fis);
			}
		}
		
		this.properties = prop;
	}
	
	public void saveToFile(String filename) throws IOException {
		
		File file = new File(filename);
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(file);
			this.properties.store(fos, null);
			
		} finally {
			CloseableUtility.safeClose(fos);
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public void setObjectProperty(Class clazz, String key, Serializable value) {
		String fullkey = makeFullKey(clazz, key);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BASE64EncoderStream b64strm = new BASE64EncoderStream(baos);
			ObjectOutputStream oos = new ObjectOutputStream(b64strm);
			// Serialize object
			oos.writeObject(value);
			
			// BASE64 encode
			oos.flush();
			b64strm.flush();
			
			// Get as BASE64 String
			String base64obj = baos.toString("UTF-8");
			this.properties.setProperty(fullkey, base64obj);
			
		} catch (IOException e) {
			throw new RuntimeException("Failed to get property: " + fullkey, e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private String makeFullKey(Class clazz, String key) {
		String fullkey = (clazz != null ? clazz.getName() + "." : "") + key;
		return fullkey;
	}
	
	@SuppressWarnings("rawtypes")
	public Object getObjectProperty(Class clazz, String key) {
		String fullkey = (clazz != null ? clazz.getName() + "." : "") + key;
		String propValue = this.properties.getProperty(fullkey);
		if (propValue == null) {
			return null;
		}
		
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(propValue.getBytes("UTF-8"));
			BASE64DecoderStream b64strm = new BASE64DecoderStream(bais);
			ObjectInputStream ois = new ObjectInputStream(b64strm);
			
			Object o = ois.readObject();
			return o;
			
		} catch (IOException e) {
			throw new RuntimeException("Failed to set property: " + fullkey, e);
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to set property: " + fullkey, e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public String getProperty(Class clazz, String key) {
		String fullkey = makeFullKey(clazz, key);
		return this.properties.getProperty(fullkey);
	}
	
	@SuppressWarnings("rawtypes")
	public void setProperty(Class clazz, String key, String value) {
		String fullkey = makeFullKey(clazz, key);
		this.properties.setProperty(fullkey, value);
	}
}
