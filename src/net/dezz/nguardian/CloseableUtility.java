package net.dezz.nguardian;

import java.io.Closeable;

public final class CloseableUtility {
	
	public static void safeClose(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
