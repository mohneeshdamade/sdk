package plm.siemens;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtility {

	public static String tmpDir = "temp";

	public ZipUtility() throws IOException {
		System.out.println("tmp dir :" + tmpDir);
		File temp = new File(tmpDir, "zippedArtifacts");
		tmpDir=temp.getPath();
		if (!temp.exists()) {
			temp.mkdirs();
		}

	}

	public static void main(String[] args) throws IOException {

		ZipUtility zipUtil = new ZipUtility();
		zipUtil.createZip("D:\\wallpapers");
	}

	public void createZip(String rootDirPath) {
		try {
			File fileToZip = new File(rootDirPath);
			FileOutputStream fos = new FileOutputStream(tmpDir + "/" + fileToZip.getName() + ".zip");
			ZipOutputStream zipOut = new ZipOutputStream(fos);
			zipFile(fileToZip, fileToZip.getName(), zipOut);

			zipOut.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void zipFile(File dir, String fileName, ZipOutputStream zipOut) throws IOException {
		if (dir.isHidden()) {
			return;
		}
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		FileInputStream fis = new FileInputStream(dir);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);

		byte[] bytes = new byte[4096];
		int length;
		while ((length = fis.read(bytes)) > 0) {
			zipOut.write(bytes, 0, length);

		}

		fis.close();
	}

}