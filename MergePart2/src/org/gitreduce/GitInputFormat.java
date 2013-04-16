package org.gitreduce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;



/**
 * To use {@link MultiFileInputFormat}, one should extend it, to return a 
 * (custom) {@link RecordReader}. MultiFileInputFormat uses 
 * {@link MultiFileSplit}s. 
 */
public class GitInputFormat extends FileInputFormat<LongWritable, Text>  {

	private int 		m_numSplitsFilled;
	private int 		m_numSplits;
	//private boolean[]  	m_nodeFilledFlags;
	
	@Override 
	public InputSplit[] getSplits (JobConf conf, int numSplits) throws IOException
	{
		m_numSplits = numSplits;
		m_numSplitsFilled = 0;

		FileSystem 	fs = FileSystem.get(conf);
		
		// Get the input file

		Path[]  	paths = Getpaths(conf);

		// Get number of nodes
		String[] 	nodes = GetActiveServersList(conf);
		int 		numNodes = nodes.length;

		// Compute number of splits per node
		int 		splitsPerNode = numSplits/numNodes;
		int 		extraSplits = numSplits % numNodes;

		// Compute size per split
		Long 		totalBranchSize = 1L; // TODO: find this
		Long 		numBranches = 6L;  // TODO: get this info;
		Long		numBranchComparisons = numBranches * (numBranches - 1) / 2;
		
		Long 		sizePerSplit = totalBranchSize * numBranchComparisons/numSplits; 

		List<GitInputSplit[]> allNodeInputSplists = new ArrayList<GitInputSplit[]>(numNodes);

		//m_nodeFilledFlags = new boolean[numNodes];
		
		for (int i = 0; i < numNodes; i++)
		{
			int 	numSplitsForCurrentNode = splitsPerNode;
			if (extraSplits > 0)
			{
				numSplitsForCurrentNode++;
				extraSplits--;
			}

			GitInputSplit[] currentNodeSplits = new GitInputSplit[numSplitsForCurrentNode];
			for (int j = 0; j < numSplitsForCurrentNode; j++)
				{
				currentNodeSplits[j] = new GitInputSplit (sizePerSplit);
				}
			
			allNodeInputSplists.add(currentNodeSplits);
		}	  

		
		ArrayList<String> unProcessedlines = new ArrayList<String>();
		
		// Read each line in the file in the paths and assign to one of the splits of each node
		for (Path currentPath : paths)
		{
			FSDataInputStream fdsis = fs.open(currentPath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fdsis));
			String line = "";
			
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty())
					continue;
				
				if (!AssignToNode(fs, nodes, allNodeInputSplists, line, false))
					unProcessedlines.add(line);
			}
			br.close();
		}
		
		// TODO: assign as per size and free location
		for (String line : unProcessedlines)
		{
			if (!AssignToNode(fs, nodes, allNodeInputSplists, line, true))
				throw new IOException("Should be Assigned on forceAssign: " + line);
		}
		
		// Consolidate results
		GitInputSplit[] returnSplits = new GitInputSplit[numSplits];
		int index = 0;
		for (GitInputSplit[] nodeSplits : allNodeInputSplists)
		{
			for (GitInputSplit currentSplit : nodeSplits)
			{
				returnSplits[index++] = currentSplit;
			}
		}
		
		return returnSplits;
	}

	
	private Path[] Getpaths(JobConf conf) throws IOException
	{
		final String logsPath = "/_logs/";
		Path[] allPaths = FileUtil.stat2Paths (listStatus(conf));
		
		List<Path> validPaths = new ArrayList<Path>();
		
		for (Path currentPath : allPaths) 
			{
			if (-1 != currentPath.getName().indexOf(logsPath))
				continue;
			
			validPaths.add(currentPath);
			}
		
		Path[] returnPaths = new Path[validPaths.size()];
		return validPaths.toArray(returnPaths);
	}

	
	private int GetNodeIndex(String[] nodes, String nodeName)
	{
		for (int i = 0; i< nodes.length; i++)
		{
			if (nodes[i].equals(nodeName))
				return i;
		}

		assert(false);
		return -1;
	}
	
	private boolean AssignToNode (FileSystem fs, String[] nodes, List<GitInputSplit[]> allNodeInputLists, String inputline, boolean forceAssign) throws IOException
	{
		String[] tokens = inputline.split("\\s+");

		assert(tokens.length == 4);

		if (tokens.length != 4)
		{
			throw new IOException ("Invalid format of line: " + inputline);
			// log ("Invalid line in input file");
		}

		String hostBranchFile = tokens[0].trim(); 
		String mergeBranchFile = tokens[1].trim();
		String parentBranchFile = tokens[2].trim();

		Long size = java.lang.Long.parseLong(tokens[3].trim());

		if (!forceAssign)
			{
			if (m_numSplitsFilled == m_numSplits) //TODO
				forceAssign = true;
			}
		
		// Try assigning to the host first;
		final String nullString = "null";
		
		
		if (!hostBranchFile.equals(nullString))
		{
			String nodeName = GetNodeNameForPath(fs, hostBranchFile);

			int nodeIndex = GetNodeIndex(nodes, nodeName);

			if (-1 == nodeIndex)
				throw new IOException ("Cannot find index for host from the hosts list");

			if (nodeIndex >= allNodeInputLists.size())
				throw new IOException ("Invalid node Index");

			GitInputSplit[] nodeSplits =  allNodeInputLists.get(nodeIndex);

			//if (false == m_nodeFilledFlags[nodeIndex] || forceAssign)
			//{
			if (AssignToSplits (nodeSplits, nodeName, hostBranchFile, mergeBranchFile, parentBranchFile, size, forceAssign))
				return true;
			//}
		}
		

		// Try assigning to branch
		if (!mergeBranchFile.equals(nullString))
		{
			String nodeName = GetNodeNameForPath(fs, mergeBranchFile);

			int nodeIndex = GetNodeIndex(nodes, nodeName);

			if (-1 == nodeIndex)
				throw new IOException ("Cannot find index for host from the hosts list");

			GitInputSplit[] nodeSplits =  allNodeInputLists.get(nodeIndex);

			//if (false == m_nodeFilledFlags[nodeIndex] || forceAssign)
			//{
			if (AssignToSplits (nodeSplits, nodeName, hostBranchFile, mergeBranchFile, parentBranchFile, size, forceAssign))
				return true;
			//}	
		}
		else if (forceAssign) 
			return true; // this means both are null no need to assign
			
		
		return false;
	}

	
	private boolean AssignToSplits 
	(
	GitInputSplit[] nodeInputSplits, 
	String 			nodeName,
	String 			hostBranchFile, 
	String 			mergeBranchFile,
	String 			parentBranchFile, 
	Long   			size,
	boolean 		forceAssign
	) throws IOException
		{
		GitInputSplit smallestSplit = null;
		
		for (GitInputSplit currentSplit : nodeInputSplits)
		{
			if (forceAssign)
			{
				// Just find out the smallest Split
				if (null == smallestSplit)
					smallestSplit = currentSplit;
				else
					if (smallestSplit.m_totalSize > currentSplit.m_totalSize)
						smallestSplit = currentSplit;
				
				continue;
			}
			
			if (currentSplit.m_totalSize + size > currentSplit.m_maxSize)
				continue;
			
			currentSplit.AddMergeInfo(nodeName, hostBranchFile, mergeBranchFile, parentBranchFile, size);
			return true;
		}
		
		if (forceAssign)
		{
			if (null == smallestSplit)
				throw new IOException ("Couldn't get a smallestSplit for node " + nodeName);
			
			smallestSplit.AddMergeInfo(nodeName, hostBranchFile, mergeBranchFile, parentBranchFile, size);
			return true;
		}
		
		return false;
		}

	
	private String GetNodeNameForPath(FileSystem fs, String path) throws IOException
	{
		Path hdfsPath = new Path(path);

		BlockLocation[] blockLocations= fs.getFileBlockLocations(fs.getFileStatus(hdfsPath), 0, path.length());
		
		if (null == blockLocations || 0 == blockLocations.length)
		{
			assert (false);
			throw new IOException("Couldn't find location for file on hdfs:" + path);
		}
		
		String[] hostNames = blockLocations[0].getHosts();
		
		if (null == hostNames || 0 == hostNames.length)
		{
			assert (false);
			// TODO: try the other blockLocations?
			throw new IOException("Couldn't find hostName for file on hdfs:" + path);
		}
		
		return hostNames[0];
	}

	private String[] GetActiveServersList(JobConf conf){

		String [] servers = null;
		try {
			JobClient jc = new JobClient(conf); 
			ClusterStatus status = jc.getClusterStatus(true);
			Collection<String> atc = status.getActiveTrackerNames();
			servers = new String[atc.size()];
			int s = 0;
			for(String serverInfo : atc){
				StringTokenizer st = new StringTokenizer(serverInfo, ":");
				String trackerName = st.nextToken();
				StringTokenizer st1 = new StringTokenizer(trackerName, "_");
				st1.nextToken();
				servers[s++] = st1.nextToken();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}

		return servers;
	}

	@Override
	public RecordReader<LongWritable, Text> getRecordReader(InputSplit split, JobConf conf,
			Reporter reporter) throws IOException {
		// TODO Auto-generated method stub
		return new GitRecordReader((GitInputSplit)split);
	}
}
