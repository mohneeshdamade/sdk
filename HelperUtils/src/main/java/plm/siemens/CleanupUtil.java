package plm.siemens;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.util.FileSystemUtils;

/**
 * Given a string representing directory, this utility should cleanup the
 * directory based on the TTL (time to live) if the difference is more than 5
 * minutes , it should scan and delete all the folders and files recursively
 * 
 * @author Mohneesh
 *
 */
public class CleanupUtil {

	private static Logger logger = Logger.getLogger(CleanupUtil.class);

	public static void main(String[] args) {

		CleanupUtil cleanup = new CleanupUtil();
		cleanup.cleanupTempFiles("D:\\Games\\Angry Birds - 2011 - PC - Cracked\\Angry_Birds_PC_Cracked\\Angry Birds\\data\\fonts\\pc_build");
	}

	public boolean cleanupTempFiles(String pathToCleanup) {

		File file = new File(pathToCleanup);

		long currentTime = System.currentTimeMillis();

		logger.info("Folder to cleanup : " + file.getPath());
		if (file.isDirectory()) {
			long lastModified = file.lastModified();
			logger.info("Last modified : " + lastModified);

			long timeElapsed = currentTime - lastModified;
			logger.info("Time Elapsed : " + timeElapsed);
			
			long ttl = TimeUnit.MINUTES.toMillis(5);
			logger.info("Time To Live : " + ttl);
			
			if(timeElapsed > ttl){
				logger.info("Performing cleanup ...");
				FileSystemUtils.deleteRecursively(file);
			}
		}

		return false;

	}

}
