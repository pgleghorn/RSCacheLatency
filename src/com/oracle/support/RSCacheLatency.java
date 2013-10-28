package com.oracle.support;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Phil Gleghorn
 * 
 */
public class RSCacheLatency {

	private List<String> memory = new ArrayList<String>();

	private long sleep;
	private String dirname;

	public static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	private String readLine(File f) {
		String line = "";
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(f));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			line = br.readLine();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return line;
	}

	private String formatCalendar(Calendar c) {
		if (c == null)
			return "null date";
		StringBuffer sb = new StringBuffer();
		sb.append(c.get(Calendar.YEAR)).append("/")
				.append(c.get(Calendar.MONTH)).append("/")
				.append(c.get(Calendar.DATE)).append("_")
				.append(c.get(Calendar.HOUR_OF_DAY)).append(":")
				.append(c.get(Calendar.MINUTE)).append(":")
				.append(c.get(Calendar.SECOND)).append(".")
				.append(c.get(Calendar.MILLISECOND)).append(",Z")
				.append(c.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000))
				.append(",D")
				.append(c.get(Calendar.DST_OFFSET) / (60 * 60 * 1000));
		return sb.toString();
	}

	private void scanDir() {
		File dir = new File(dirname);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".fst");
			}
		};

		File[] files = dir.listFiles(filter);
		boolean first = true;
		for (File f : files) {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(f.lastModified());
			String lastModified = formatCalendar(c);
			String now = formatCalendar(Calendar.getInstance());

			String key = f.getName() + lastModified;
			if (!memory.contains(key)) {
				memory.add(key);
				if (first) {
					System.out.println("");
					first = false;
				}
				String fileContents = readLine(f);
				Calendar innerTimestamp = null;
				try {
					innerTimestamp = Calendar.getInstance();
					innerTimestamp.setTimeInMillis(Long.parseLong(fileContents
							.split("&")[0].split("=")[1]));
				} catch (Exception e) {
				}
				System.out.println(padRight(now, 32)
						+ padRight(lastModified, 32)
						+ padRight(fileContents, 32)
						+ padRight(formatCalendar(innerTimestamp), 32)
						+ f.getName());
			}
		}
	}

	private void doWork() {
		System.out.println();
		System.out.println(padRight("current time", 32)
				+ padRight("file lastmodified", 32)
				+ padRight("file contents", 32)
				+ padRight("file inner timestamp", 32) + "file name");
		System.out.println(padRight("============", 32)
				+ padRight("=================", 32)
				+ padRight("=============", 32)
				+ padRight("====================", 32) + "=========");
		while (true) {
			scanDir();
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.print(".");
		}
	}

	private void dumpInfo() {
		System.out.println("Sleep duration " + sleep + " milliseconds");
		System.out.println("Checking directory " + dirname);
		System.out.println("\nNetwork interfaces:");
		try {
			Enumeration<NetworkInterface> nife = NetworkInterface
					.getNetworkInterfaces();
			while (nife.hasMoreElements()) {
				NetworkInterface nif = nife.nextElement();
				if (nif.isUp()) {
					Enumeration<InetAddress> ias = nif.getInetAddresses();
					System.out.println(nif.getName() + " ("
							+ nif.getDisplayName() + ") ");
					while (ias.hasMoreElements()) {
						InetAddress ia = ias.nextElement();
						String ipType = "IPv4";
						if (ia instanceof Inet6Address)
							ipType = "IPv6";
						System.out.println("    " + ipType + " "
								+ ia.getCanonicalHostName() + " ("
								+ ia.getHostAddress() + ")");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("\nSystem properties:");
		Enumeration enumeration = System.getProperties().keys();
		List<String> list = Collections.list(enumeration);
		Collections.sort(list);
		for (String propname : list) {
			System.out.println(propname + "=" + System.getProperty(propname));
		}
	}

	private void setupArgs(String[] args) {
		if (args == null || args.length < 2) {
			System.out
					.println("This utility tries to detect latency issues across cluster nodes. \n"
							+ "Run it simultaneously on two or more cluster nodes and they will \n"
							+ "report exactly when changes are seen on the clustersync folder. \n"
							+ "Author phil.gleghorn@oracle.com");
			System.out.println("Usage: java -jar rscachelatency.jar sleepInMilliseconds directoryName");
			System.out.println(" e.g.: java -jar rscachelatency.jar 50 /home/csuser/Shared/clustersync");
			System.exit(0);
		}
		sleep = Long.parseLong(args[0]);
		dirname = args[1];

	}

	public static void main(String[] args) {
		RSCacheLatency rs = new RSCacheLatency();
		rs.setupArgs(args);
		rs.dumpInfo();
		rs.doWork();
	}

}
