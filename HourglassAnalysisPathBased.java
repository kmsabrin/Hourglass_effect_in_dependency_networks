import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class HourglassAnalysisPathBased {
	private static int pathBasedFlatCore(String pathFile) throws Exception {
		Scanner scanner = new Scanner(new File(pathFile));		
		HashSet<String> paths = new HashSet();
		int numPaths = 0;
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			String[] path = line.split("\\s+");
			paths.add(line);
			++numPaths;
		}
		scanner.close();
		
		int size = (int)(paths.size() * (1.0 - pathCoverageTau));
		double startSize = paths.size();
		int flatCoreSize = 0;
		double cumulativePathCover = 0;
		while (true) {
			HashMap<String, Integer> maxPathBasedCentrality = new HashMap();
			for (String line : paths) {
				String[] tokens = line.split("\\s+");
				for (int i = 0; i < tokens.length; ++i) {
					if (i == 0 || i == tokens.length - 1) {
						String r = tokens[i];
						if (maxPathBasedCentrality.containsKey(r)) {
							maxPathBasedCentrality.put(r, maxPathBasedCentrality.get(r) + 1);
						} else {
							maxPathBasedCentrality.put(r, 1);
						}
					}
				}
			}
			
			int max = 0;
			HashSet<String> maxPathCentralityNode = new HashSet();
			for (String v : maxPathBasedCentrality.keySet()) {
				if (maxPathBasedCentrality.get(v) > max) {
					maxPathCentralityNode.clear();
					maxPathCentralityNode.add(v);
					max = maxPathBasedCentrality.get(v);
				}
			}
			
			cumulativePathCover += max;
			HashSet<String> removePaths = new HashSet();
			for (String line : paths) {
				String[] tokens = line.split("\\s+");
				for (int i = 0; i < tokens.length; ++i) {
					if (i == 0 || i == tokens.length - 1) {
						String r = tokens[i];					
						if (maxPathCentralityNode.contains(r)) {
							removePaths.add(line);
						}
					}
				}
			}

			paths.removeAll(removePaths);
			++flatCoreSize;
			if (cumulativePathCover >= numPaths * pathCoverageTau) {
				break;
			}
		}
		return flatCoreSize;
	}
	
	private static int pathBasedHourglassCore(String pathFile) throws Exception {
		HashSet<String> paths  = new HashSet();
		int numPaths = 0;
		Scanner scanner = new Scanner(new File(pathFile));
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			String[] path = line.split("\\s+");
			paths .add(line);
			++numPaths;
		}
		scanner.close();
		
		int realCoreSize = 0;
		double cumulativePathCover = 0;
		while (true) {
			HashMap<String, Integer> maxPathBasedCentrality = new HashMap();
			for (String line : paths ) {
				String[] tokens = line.split("\\s+");
				for (String r : tokens) {
					if (maxPathBasedCentrality.containsKey(r)) {
						maxPathBasedCentrality.put(r, maxPathBasedCentrality.get(r) + 1);
					}
					else {
						maxPathBasedCentrality.put(r, 1);
					}
				}
			}
			
			int max = 0;
			HashSet<String> maxPathCentralityNode = new HashSet();
			for (String v : maxPathBasedCentrality.keySet()) {
				if (maxPathBasedCentrality.get(v) > max) {
					maxPathCentralityNode.clear();
					maxPathCentralityNode.add(v);
					max = maxPathBasedCentrality.get(v);
				}
			}
			
			cumulativePathCover += max;
			for (String v : maxPathCentralityNode) {
				System.out.println("Core node: "  + v + "\t" + "Coverage: " + (max * 1.0 / numPaths));
			}

			HashSet<String> removePaths = new HashSet();
			for (String line : paths ) {
				String[] tokens = line.split("\\s+");
				for (String r : tokens) {
					if (maxPathCentralityNode.contains(r)) {
						removePaths.add(line);
					}
				}
			}
			paths .removeAll(removePaths);
			
			++realCoreSize;			
			if (paths .size() <= 0 || cumulativePathCover >= numPaths * pathCoverageTau) {
				break;
			}
		}
		return realCoreSize;
	}
	
	public static double pathCoverageTau = 0.9;
	
	public static void main(String[] args) throws Exception {	
		String pathFile = args[0];
		pathCoverageTau = Double.parseDouble(args[1]);
		int realCoreSize = pathBasedHourglassCore(pathFile);
		int flatCoreSize = pathBasedFlatCore(pathFile);
		System.out.println("Original core size: " + realCoreSize + "\t" + "Flat core size: " + flatCoreSize);
		System.out.println("H_score: " + (1.0 - (realCoreSize * 1.0 / flatCoreSize)));
	}
}