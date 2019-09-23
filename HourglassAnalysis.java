import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;


public class HourglassAnalysis {
  private static class DependencyDAG {
    private HashSet<String> nodes;
    private HashSet<String> targets;
    private HashSet<String> sources;
    private HashMap<String, HashSet<String>> serves;
    private HashMap<String, HashSet<String>> depends;

    private HashMap<String, Double> numOfTargetPath;
    private HashMap<String, Double> numOfSourcePath;
    private HashMap<String, Double> nodePathThrough;
    private double nTotalPath;

    private double pathCoverageTau = 0.9;
    private HashSet<String> coreNodes;
    private HashSet<String> skipNodes;

    public static boolean processingFlat = false;
    private HashMap<String, Double> edgeWeights;

    private HashMap<String, Integer> sourceTargetPairShortestPath;

    public DependencyDAG() {
      nodes = new HashSet();
      serves = new HashMap();
      depends = new HashMap();
      targets = new HashSet();
      sources = new HashSet();
      numOfTargetPath = new HashMap();
      numOfSourcePath = new HashMap();
      nodePathThrough = new HashMap();
      skipNodes = new HashSet();
      edgeWeights = new HashMap();
      sourceTargetPairShortestPath = new HashMap();
    }

    public DependencyDAG(String dependencyGraphFilePath, String sourceFilePath, String targetFilePath, String threshold)
        throws Exception {
      this();
      pathCoverageTau = Double.parseDouble(threshold);
      loadNetwork(dependencyGraphFilePath);
      loadSources(sourceFilePath);
      loadTargets(targetFilePath);
      addSuperSourceTarget();
      getPathStats();
      getCore();
      if (!processingFlat) {
        getFlatNetwork();
      }
    }

    public DependencyDAG(String dependencyGraphFilePath, String sourceFilePath, String targetFilePath, int hopKount)
        throws Exception {
      this();
      loadNetwork(dependencyGraphFilePath);
      loadSources(sourceFilePath);
      loadTargets(targetFilePath);
      shortestPathBFS();
      traverseAllPaths(hopKount);
    }

    private void addEdge(String server, String dependent) {
      nodes.add(dependent);
      nodes.add(server);
      if (serves.containsKey(server)) {
        serves.get(server).add(dependent);
      } else {
        HashSet<String> hs = new HashSet();
        hs.add(dependent);
        serves.put(server, hs);
      }
      if (depends.containsKey(dependent)) {
        depends.get(dependent).add(server);
      } else {
        HashSet<String> hs = new HashSet();
        hs.add(server);
        depends.put(dependent, hs);
      }
    }

    private void removeEdge(String server, String dependent) {
      serves.get(server).remove(dependent);
      depends.get(dependent).remove(server);
    }

    private void addSuperSourceTarget() {
      for (String s : sources) {
        addEdge("superSource", s);
        if (processingFlat) {
          edgeWeights.put("superSource#" + s, 1.0);
        }
      }
      for (String t : targets) {
        addEdge(t, "superTarget");
        if (processingFlat) {
          edgeWeights.put(t + "#superTarget", 1.0);
        }
      }
    }

    private void loadTargets(String fileName) throws Exception {
      Scanner scanner = new Scanner(new File(fileName));
      while (scanner.hasNext()) {
        targets.add(scanner.next());
      }
      scanner.close();
    }

    private void loadSources(String fileName) throws Exception {
      Scanner scanner = new Scanner(new File(fileName));
      while (scanner.hasNext()) {
        sources.add(scanner.next());
      }
      scanner.close();
    }

    private void loadNetwork(String fileName) throws Exception {
      Scanner scanner = new Scanner(new File(fileName));
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String tokens[] = line.split("\\s+");
        String server = tokens[0];
        String dependent = tokens[1];
        if (processingFlat) {
          double weight = Double.parseDouble(tokens[2]);
          edgeWeights.put(server + "#" + dependent, weight);
        }
        addEdge(server, dependent);
      }
      scanner.close();
    }

    private void sourcePathsTraverse(String node) {
      if (numOfSourcePath.containsKey(node)) { // node already traversed
        return;
      }
      double nPath = 0;
      if (!skipNodes.contains(node)) {
        if (node.equals("superSource")) {
          ++nPath;
        }
        if (depends.containsKey(node)) {
          for (String s : depends.get(node)) {
            sourcePathsTraverse(s);
            double weight = 1;
            if (processingFlat) {
              weight = edgeWeights.get(s + "#" + node);
            }
            nPath += numOfSourcePath.get(s) * weight;
          }
        }
      }
      numOfSourcePath.put(node, nPath);
    }

    private void targetPathsTraverse(String node) {
      if (numOfTargetPath.containsKey(node)) { // node already traversed
        return;
      }
      double nPath = 0;
      if (!skipNodes.contains(node)) {
        if (node.equals("superTarget")) {
          ++nPath;
        }
        if (serves.containsKey(node)) {
          for (String s : serves.get(node)) {
            targetPathsTraverse(s);
            double weight = 1;
            if (processingFlat) {
              weight = edgeWeights.get(node + "#" + s);
            }
            nPath += numOfTargetPath.get(s) * weight;
          }
        }
      }
      numOfTargetPath.put(node, nPath);
    }

    private void pathStatisticsHelper() {
      numOfSourcePath.clear();
      for (String s : nodes) {
        sourcePathsTraverse(s);
      }
      numOfTargetPath.clear();
      for (String s : nodes) {
        targetPathsTraverse(s);
      }
    }

    private void getPathStats() {
      pathStatisticsHelper();
      nTotalPath = 0;
      for (String s : nodes) {
        double nPath = 0;
        if (numOfSourcePath.containsKey(s) && numOfTargetPath.containsKey(s)) {
          nPath = numOfSourcePath.get(s) * numOfTargetPath.get(s);
        }
        nodePathThrough.put(s, nPath);
      }
      nTotalPath = nodePathThrough.get("superSource");
    }

    private boolean isSuperNode(String n) {
      if (n.equals("superSource") || n.equals("superTarget")) {
        return true;
      }
      return false;
    }

    public void printNetworkProperties() throws Exception {
			//PrintWriter pw = new PrintWriter(new File("hourglassAnalysis.txt"));
      for (String s : nodes) {
        if (isSuperNode(s)) {
          continue;
        }
        System.out.println(s + "\tComplexity: " + numOfSourcePath.get(s) + "\tGenerality: " + numOfTargetPath.get(s)
            + "\tPath_centrality: " + nodePathThrough.get(s));
				//System.out.println(s + "\t" + nodePathThrough.get(s));
      }
      System.out.println("Total_paths: " + nTotalPath);
      System.out.println("Core_size: " + coreNodes.size() + "\t" + "Core_set: " + coreNodes);
			//pw.close();
    }

    private void getCore() {
      greedyTraverse(0, nTotalPath);
      // resetting everything
      skipNodes.clear();
      getPathStats();
    }

    private void greedyTraverse(double cumulativePathCovered, double nPath) {
      if (!(cumulativePathCovered < nPath * pathCoverageTau)) {
        coreNodes = new HashSet(skipNodes);
        return;
      }
      double maxPathCovered = -1;
      String maxPathCoveredNode = "";
      for (String s : nodes) {
        if (isSuperNode(s)) {
          continue;
        }
        double numPathCovered = nodePathThrough.get(s);
        if (numPathCovered > maxPathCovered) {
          maxPathCovered = numPathCovered;
          maxPathCoveredNode = s;
        }
      }
      skipNodes.add(maxPathCoveredNode);
      getPathStats();
      if (!processingFlat) {
        //System.out.println(maxPathCoveredNode + "\t" + ((cumulativePathCovered + maxPathCovered) / nPath));
      }
      greedyTraverse(cumulativePathCovered + maxPathCovered, nPath);
    }

    private void getFlatNetwork() throws Exception {
      PrintWriter pw = new PrintWriter(new File("flat.txt"));
      for (String s : sources) {
        removeEdge("superSource", s);
      }
      for (String s : sources) {
        addEdge("superSource", s);
        getPathStats();
        for (String r : targets) {
          if (nodePathThrough.get(r) > 0) {
            pw.println(s + "\t" + r + "\t" + numOfSourcePath.get(r));
          }
        }
        removeEdge("superSource", s);
      }
      pw.close();
      // resetting everything
      for (String s : sources) {
        addEdge("superSource", s);
      }
      getPathStats();
    }

    private void traverseAllPathsHelper(String node, String targetNode, int len, ArrayList<String> pathNodes,
        PrintWriter pw, int k) {
      if (pathNodes.size() > len + k) {
        return; // +k hop than shortest path
      }
      //if (pathNodes.size() > k) return; // length restricted path

      if (node.equals(targetNode)) {
        for (String s : pathNodes) {
          pw.print(s + " ");
        }
        pw.println();
        return;
      }

      if (!serves.containsKey(node)) {
        return;
      }

      for (String s : serves.get(node)) {
        if (pathNodes.contains(s)) {
          continue;
        }
        pathNodes.add(s);
        traverseAllPathsHelper(s, targetNode, len, pathNodes, pw, k);
        pathNodes.remove(s);
      }
    }

    private void traverseAllPaths(int hopKount) throws Exception {
      PrintWriter pw = new PrintWriter(new File("paths.txt"));
      for (String s : sources) {
        for (String r : targets) {
          if (!sourceTargetPairShortestPath.containsKey(s + "#" + r)) {
            continue;
          }
          ArrayList<String> pathNodes = new ArrayList();
          pathNodes.add(s);
          traverseAllPathsHelper(s, r, sourceTargetPairShortestPath.get(s + "#" + r) + 1, pathNodes, pw, hopKount);
        }
      }
      pw.close();
    }

    private void shortestPathBFS() throws Exception {
      for (String s : sources) {
        Queue<String> bfsQ = new LinkedList();
        Queue<Integer> depthQ = new LinkedList();
        HashSet<String> visited = new HashSet();

        bfsQ.add(s);
        depthQ.add(0);
        visited.add(s);

        while (!bfsQ.isEmpty()) {
          String n = bfsQ.poll();
          int depth = depthQ.poll();
          if (targets.contains(n)) {
            String pair = s + "#" + n;
            sourceTargetPairShortestPath.put(pair, depth);
          }

          if (serves.containsKey(n)) {
            for (String r : serves.get(n)) {
              if (!visited.contains(r)) {
                bfsQ.add(r);
                depthQ.add(depth + 1);
                visited.add(r);
              }
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 4) {
	System.out.println("Incorrect number of arguments");
	System.exit(-1);
    }
    if (args[3].startsWith("+")) {
        DependencyDAG dependencyDAG = new DependencyDAG(args[0], args[1], args[2], Integer.parseInt(args[3]));
    }
    else {
    	DependencyDAG dependencyDAG = new DependencyDAG(args[0], args[1], args[2], args[3]);
    	System.out.println("Original_network");
    	dependencyDAG.printNetworkProperties();
    	String flatDAGFile = "flat.txt";
    	DependencyDAG.processingFlat = true;
    	DependencyDAG flatDAG = new DependencyDAG(flatDAGFile, args[1], args[2], args[3]);
    	System.out.println("Flat_network");
    	flatDAG.printNetworkProperties();
    	double hScore = (1.0 - dependencyDAG.coreNodes.size() * 1.0 / flatDAG.coreNodes.size());
    	System.out.println("H_score: " + hScore);
    }
  }
}
