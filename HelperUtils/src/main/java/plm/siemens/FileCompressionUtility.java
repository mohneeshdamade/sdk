package plm.siemens;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

/**
 * @author Mohneesh
 *
 */
@Component
public class FileCompressionUtility {

	private static Logger logger = Logger.getLogger(FileCompressionUtility.class);

	/**
	 * TODO: Initialize this temp directory with some configuration parameter
	 * and use the temp folder as a fallback value.
	 */
	private static String TMP_DIR = "temporary";

	private static final String ZIP = ".zip";

	private static long lastCleanupTime;
	/**
	 * Buffer size to compress a particular zip file in the folder This should
	 * never be zero, it should be big enough to accommodate every file inside
	 * the folder looking at the current structure we do not expect any file
	 * bigger than 4MB.
	 */
	private static int BUFFER = 4096;

	private static boolean isInitialized = false;

	@PostConstruct
	static void init() {
		logger.info("Initializing FileCompression Utility...");
		File temp = new File(TMP_DIR);
		temp.mkdir();
		TMP_DIR = temp.getPath();
		if (!temp.exists()) {
			temp.mkdirs();
			isInitialized = true;
		}
	}

	public static void main(String[] args) throws Exception {

		FileCompressionUtility zipUtil = new FileCompressionUtility();
		// zipUtil.init();
		byte[] bytes = FileCompressionUtility.createZip("D:\\netflix wiki");

		FileOutputStream out = new FileOutputStream(new File("D:\\zipped\\zip1.zip"));
		out.flush();
		out.close();

		System.out.println(bytes.length);
	}

	/**
	 * Create a zip artifact of the project path which is passed along
	 * 
	 * @param rootDirPath
	 *            This should be a unique rootDirPath in the temp folder which
	 *            represent the root of the project folder for which a zip has
	 *            to be generated.
	 * @throws Exception
	 */
	public static byte[] createZip(String rootDirPath)
			throws IllegalArgumentException, FileNotFoundException, IOException {

		if (rootDirPath == null || rootDirPath.equalsIgnoreCase("") || rootDirPath.equalsIgnoreCase(".")) {
			logger.error("Invalid rootDirPath passed : " + rootDirPath);
			throw new IllegalArgumentException("Invalid rootDirPath passed : " + rootDirPath);
		}

		File fileToZip = new File(rootDirPath);
		// Check if the path specified exists or not
		if (!fileToZip.exists()) {
			throw new IllegalArgumentException("Directory does not exist : " + rootDirPath);
		}

		if (isInitialized == false) {
			logger.debug("Init method was not called during startup :: Initializing now...");
			init();
		}

		// String zippedArtifactLocation = TMP_DIR + "\\" + fileToZip.getName()
		// + ZIP;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {

			logger.info("Generating zip artifact for project ::: " + rootDirPath);
			// logger.info("Compressed file will be loacated at ::: " +
			// zippedArtifactLocation);

			zipFolder(fileToZip, fileToZip.getName(), zipOut);

			// logger.info("Generated ZIP artifact at location : " +
			// zippedArtifactLocation);

		} catch (FileNotFoundException e) {
			logger.error("File not found :: " + e.getMessage());
			e.printStackTrace();
			throw new FileNotFoundException("Specified folder not found");
		} catch (IOException e) {
			logger.error("IO Exception :: " + e.getMessage());
			e.printStackTrace();
			throw new IOException("IO Exception occured");
		} catch (Exception e) {
			/**
			 * TODO: Perform cleanup since the artifact is corrupted And throw
			 * an exception to indicate failure generating the zip correctly
			 */
			logger.error("Exception occured while generating zip :: " + e.getMessage());
			e.printStackTrace();

		}
		return baos.toByteArray();
	}

	private static void zipFolder(File dir, String fileName, ZipOutputStream zipOut) throws IOException {
		if (dir.isHidden()) {
			logger.error("Specified file / directory is hidden / not present :: " + fileName);
			return;
		}
		if (dir.isDirectory()) {

			File[] children = dir.listFiles();
			for (File childFile : children) {
				if (childFile.isDirectory() && childFile.list().length == 0) {
					/**
					 * For any subfolder which is empty needs to be added
					 * explicitly because by default zip util will not add empty
					 * folders in zip package.
					 */
					zipOut.putNextEntry(new ZipEntry(fileName + "/" + childFile.getName() + "/"));

				}
				zipFolder(childFile, fileName + "/" + childFile.getName(), zipOut);
			}

			return;
		}

		FileInputStream fis = new FileInputStream(dir);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		logger.info("Adding file : " + zipEntry.getName());

		byte[] bytes = new byte[BUFFER];
		int length;

		while ((length = fis.read(bytes)) > 0) {
			zipOut.write(bytes, 0, length);

		}

		zipOut.closeEntry();
		fis.close();
	}

	private long getDirectorySize(String rootDir) {
		Path folder = Paths.get(rootDir);
		logger.info("Path to walk is : " + folder.toString());
		long size = 0;
		try {
			size = Files.walk(folder).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return size;
	}

	/**
	 * Utility method to return the temporary folder for project generation. It
	 * creates a folder named with the UUID being passed inside the temp
	 * directory.This can be used by the orchestrator during the project
	 * generation phase.
	 * 
	 * @param UUID
	 *            Unique ID which will identify a particular project request
	 *            uniquely
	 * @return Returns an absolute path to the folder created.
	 * @throws IOException
	 *             Throws an error in case of situations there are restrictions
	 *             to create a folder on the instance
	 */
	public static String getTemporaryFolderForProjectGeneration(String uuid) throws NullPointerException, IOException {

		if (uuid == null) {
			throw new NullPointerException("UUID passed is NULL");
		}

		String absolutePathToFolder = TMP_DIR + "\\" + uuid + "\\";
		logger.debug("Attempting to create temporary folder :: " + absolutePathToFolder);

		File file = new File(absolutePathToFolder);
		file.mkdir();

		if (file.exists() && file.isDirectory()) {
			logger.info("Created temporary folder with absolute path :: " + file.getAbsolutePath());
			return file.getAbsolutePath();
		} else {
			throw new IOException("Problem creating folder with UUID : " + uuid);
		}

	}

	/**
	 * 
	 * @return
	 */
	private static boolean isThresholdLimitExceededForCleanup() {

		boolean doCleanup = false;
		long thresholdSizeInMB = 512;
		long thresholdSizeInBytes = 1024 * 1024 * thresholdSizeInMB;
		// Check the disk space in TMPDIR
		File file = new File(TMP_DIR);
		long partitionFreeSpace = file.getFreeSpace();
		if (partitionFreeSpace == 0L) {
			logger.error("Could not retrieve free space information,requires manual cleanup !! " + TMP_DIR);
		} else {
			if (partitionFreeSpace < thresholdSizeInBytes) {
				doCleanup = true;
			}
		}
		return doCleanup;

	}

	/**
	 * 
	 * @return
	 */
	public static boolean cleanupTempFolderForExceededTTLFolders() {

		if (isThresholdLimitExceededForCleanup()) {
			File tempFolder = new File(TMP_DIR);

			long currentTime = System.currentTimeMillis();
			lastCleanupTime = currentTime;

			long ttl = TimeUnit.MINUTES.toMillis(5);
			logger.info("Time To Live : " + ttl);

			try {
				if (tempFolder.exists() && tempFolder.isDirectory()) {
					for (File file : tempFolder.listFiles()) {
						// For each folder in temp file, check the timestamp and
						// delete
						// the folders exceeding the TTL Limit

						logger.info("Attempting cleanup on folder  ::: " + file.getAbsolutePath());
						if (file.isDirectory()) {
							long lastModified = file.lastModified();
							logger.debug("Last modified : " + lastModified);

							long timeElapsed = currentTime - lastModified;
							logger.debug("Time Elapsed : " + timeElapsed);

							if (timeElapsed > ttl) {
								logger.info("Deleting folder :: " + file.getAbsolutePath());
								FileSystemUtils.deleteRecursively(file);
							}
						}
					}
				} else {
					logger.debug("No temp folder exists on this path or the TMP path is invalid ::: " + TMP_DIR);
					return false;
				}
			} catch (Exception e) {
				logger.error("Exception occured while cleanup , requires manual cleanup!!");
				e.printStackTrace();
			}
			return true;
		}
		logger.info("No cleanup performed since temp directory have enough space.");
		return false;

	}

}