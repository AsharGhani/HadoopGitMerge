import java.util.List;

import org.eclipse.egit.github.core.Repository;


public class GitBranchNamesExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String userName = javax.swing.JOptionPane.showInputDialog("Please Enter Github username");
		if (null == userName || userName.isEmpty())
			return;
		
		String repositoryName = javax.swing.JOptionPane.showInputDialog("Please enter Repository Name");
		
		if (null == repositoryName || userName.isEmpty()) 
			return;
		
		Repository repo = GitHelper.GetRepository (userName, null, repositoryName);
		
		if (null == repo)
			return;
		
		//String repositoryURL = repo.getCloneUrl();
		
		List<String> branchList = GitHelper.GetBranches (repo);
		
		String message = "";
		
		for (String branchName : branchList)
		{
			message += branchName + "\n";
		}
		
		javax.swing.JOptionPane.showMessageDialog(null, message);
	}
}
