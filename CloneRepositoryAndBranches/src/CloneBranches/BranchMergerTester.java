package CloneBranches;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.service.RepositoryService; 

import Utils.*;
import Utils.Constants.Relationship;

public class BranchMergerTester {
	
	private static RepositoryService s_repositoryService;
	
	public static void main(String[] args) throws IOException, OperationException
	{
		String userName = JOptionPane.showInputDialog("Please Enter Github username");
		if (null == userName || userName.isEmpty())
			return;
		
		String repositoryName = JOptionPane.showInputDialog("Please enter Repository Name");
		
		if (null == repositoryName || userName.isEmpty()) 
			return;
		
		String dir = JOptionPane.showInputDialog("Please enter working directory to checkout", "/host/CS_848_Project/Testing/CloneWorkDir/");
		if (dir.isEmpty())
			dir = "/host/CS_848_Project/Testing/CloneWorkDir/";
		
		if (!dir.endsWith("/"))
			dir += "/";
		
		GetAllBranchesOfRepostiroyAndParents(userName, repositoryName, dir, true);
	}
	
	public static void GetAllBranchesOfRepostiroyAndParents(String userName, String repositoryName, String workDirectory, boolean resetMerged) throws IOException, OperationException
	{
		assert (null != userName && !userName.isEmpty());		
		assert (null != repositoryName && !repositoryName.isEmpty());
		
		Repository mainRepo = GetRepository (userName, null, repositoryName);		
		if (null == mainRepo)
			return;
		
		HashMap<Repository, List<String>> repositoryToBranch = new HashMap<Repository, List<String>>();
	
		GetForksAndBranches (mainRepo, repositoryToBranch);
			
		HashMap<String, String> repoAndBranchList = new HashMap<String, String>();
		
		// Clone/Update all branches
		for (Repository currRepo : repositoryToBranch.keySet())
		{
			String currRepositoryURL = currRepo.getCloneUrl();
			String currRepositoryName =  currRepo.getName();
			
			for (String currBranchName : repositoryToBranch.get(currRepo))
			{
				repoAndBranchList.put (currRepositoryName + File.separatorChar + currBranchName, currRepositoryURL);
			
				String currWorkDirectory = getWorkDirectoryForBranch (workDirectory, currRepositoryName, currBranchName);

				try {
					BranchExtractor.createLocalRepository
						(
						currRepositoryURL,
						currWorkDirectory,
						currBranchName
					    );
				} catch (OperationException e1) {
					//_log.info("BranchMerger::MergeBraches(..) - ERROR: failed to update local repo and check cache error in getBranchesMergeRelationship");
					return /*Relationship.ERROR + " " + e1.getMessage()*/;
				} catch (IOException e2) {
					//_log.info("BranchMerger::MergeBraches(..) - ERROR: failed to update local repo and check cache error in getBranchesMergeRelationship");
					return /*Relationship.ERROR + " " + e2.getMessage()*/;
				}
			}
		}
		
		boolean[][] results = new boolean[repoAndBranchList.size()][repoAndBranchList.size()];
		
		int hostIndex = -1;
		
		String outDir = workDirectory + "/"+ repositoryName + "InputMergeMap/";
		File directory = new File(outDir); 
		if (!directory.exists())
			{
			boolean created = directory.mkdirs();
			if (created);
			}
		
		FileWriter fileWriter = new FileWriter(outDir + "directoryPaths.txt");
		
		BufferedWriter outStream = new BufferedWriter(fileWriter); 
		
		List<String> processedBranches = new ArrayList<String>();
		for (String hostRepoAndBranch : repoAndBranchList.keySet())
		{
			hostIndex++; 
			String hostBranchDir	= getWorkDirectoryForBranch (workDirectory, null, hostRepoAndBranch);			
			String hostBranch 		= hostRepoAndBranch.substring(hostRepoAndBranch.lastIndexOf (File.separatorChar)+1, hostRepoAndBranch.length());
			String hostRepo 		= hostRepoAndBranch.substring(0, hostRepoAndBranch.lastIndexOf (File.separatorChar));
			String repositoryURL 	= repoAndBranchList.get(hostRepoAndBranch);
			
			assert (repositoryURL != null);
			
			int branchIndex = -1;
			
			processedBranches.add(hostBranch);
			for (String mergeRepoAndBranch : repoAndBranchList.keySet())
			{
				branchIndex++;
				if (hostIndex == branchIndex)
					{
					results[hostIndex][branchIndex] = false;
					continue;
					}
				
				String mergeBranchDir = getWorkDirectoryForBranch (workDirectory, null, mergeRepoAndBranch);						
				String mergeBranch = mergeRepoAndBranch.substring(mergeRepoAndBranch.lastIndexOf (File.separatorChar)+1, mergeRepoAndBranch.length());
				
				if (containsKey(processedBranches, mergeBranch ))
					continue;
				
				String parentSha1 = BranchExtractor.getParent (hostBranchDir, hostBranch, mergeBranch);
				
				String parentDirName = workDirectory + hostRepo + File.separatorChar +   "Parent" + hostBranch + "-" + mergeBranch;
				
				if (BranchExtractor.cloneFromLocalRepository (hostBranchDir, parentDirName))
					BranchExtractor.checkoutChangeSet (parentDirName, parentSha1);
				
				outStream.write(hostBranchDir + " " + mergeBranchDir + " " + parentDirName );
				if (branchIndex != repoAndBranchList.size())
					outStream.newLine();
			}
		}
		
		outStream.close();
		fileWriter.close();
		/*String[] branchNames = new String [repoAndBranchList.size()];
		branchNames = repoAndBranchList.keySet().toArray(branchNames);
	
		MergeResultsDisplay resultsDisplay = new MergeResultsDisplay (branchNames, results, repoAndBranchList.size());
		resultsDisplay.toString();*/
	}
	
	static boolean containsKey(List<String> arrayString, String key)
	{
		for (String currentKey: arrayString)
		{
			if (currentKey.equals(key))
				return true;
		}
		return false;
	}
	
	private static String getWorkDirectoryForBranch (String workDirectory, String repositoryName, String branchName)
	{
		String returnString = workDirectory;
		if (!workDirectory.endsWith(File.separator))
			returnString += File.separatorChar;
		
		if (null != repositoryName && !repositoryName.isEmpty())
			returnString += repositoryName + File.separatorChar;
		
		if (null != branchName && !branchName.isEmpty())
			returnString += branchName + File.separatorChar;
		
		return returnString;
	}
	
	protected static RepositoryService GetRepositoryService ()
	{
		if (null == s_repositoryService)
			s_repositoryService = new RepositoryService();
		
		return s_repositoryService;
	}
	
	public static Repository GetRepository (String user, String pw, String repositoryName)
	{
		// Authenticate

		// Get the repository
		Repository repo = null;
		try {
			repo = GetRepositoryService().getRepository (user, repositoryName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return repo;
	}
		
	private static List<String> GetBranches (Repository repo)
	{
		assert (null != repo);

		List<String> branchesList = new ArrayList<String>();
				
		// Get Branches/forks
		//int forks = repo.getForks();
		//if (0 == forks)
		//	return branchesList;
		
		
		
		//if (forksList.isEmpty())
		//	return branchesList;
		
		List<RepositoryBranch> branches;
		try{
			branches = GetRepositoryService().getBranches (repo);
		} catch (IOException e) {
			e.printStackTrace();
			return branchesList;
		}
		
		for(RepositoryBranch repoBranch : branches)
		{
			if (isBranchValid (repoBranch))
				branchesList.add (repoBranch.getName());
		}
		
		return branchesList;
	}
	
	private static void GetForksAndBranches(Repository mainRepo, HashMap<Repository, List<String>> repositoryToBranchMap)
	{
		List<String> branchList = GetBranches (mainRepo);
		repositoryToBranchMap.put (mainRepo, branchList);
		
		List<Repository> forksList;
		try {
			forksList = GetRepositoryService().getForks (mainRepo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		for (Repository fork : forksList)
		{
			List<String> forkBranchList = GetBranches(fork);
			
			// TODO: Suppress duplicate Branches across forks
			
			
			if (null != branchList)
				repositoryToBranchMap.put (fork, forkBranchList);
		}
	}
	
	public static boolean isForkValid(Repository mainRepo, Repository fork)
	{
		// TODO: implement
		return true;
	}
	
	public static boolean isBranchValid (RepositoryBranch repoBranch)
	{
		// TODO: implement
		return true;
	}

}
