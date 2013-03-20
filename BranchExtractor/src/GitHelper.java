import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.service.RepositoryService; 


public class GitHelper {
	
	private static RepositoryService s_repositoryService;
	
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
		
	public static List<String> GetBranches (Repository repo)
	{
		assert (null != repo);

		List<String> branchesList = new ArrayList<String>();
				
		// Get Branches/forks
		//int forks = repo.getForks();
		//if (0 == forks)
		//	return branchesList;
		
		/*List<Repository> forksList;
		try {
			forksList = GetRepositoryService().getForks (repo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return branchesList;
		}*/
		
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
	
	public static boolean isBranchValid (RepositoryBranch repoBranch)
	{
	// TODO: implement
	return true;
	}
}
