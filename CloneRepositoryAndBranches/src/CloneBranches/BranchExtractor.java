package CloneBranches;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Utils.*;
import Utils.RunIt.*;
import Utils.Constants.*;

public class BranchExtractor {
	
	// TODO: logging
	private static String _executablePath;
	
	private static void Initialize()
	{
		_executablePath = RunIt.getExecutable(Constants.GIT);
	}
	
	
	private static String GetExecutablePath()
	{
		if (null == _executablePath)
			Initialize();
		
		return _executablePath;
	}
	
	
	public static String MergeBranches
	(
	String repositoryURL,
	String hostBranch,
	String hostBranchSourceCheckoutLocation,
	String mergeBranch,
	String mergeBranchSourceCheckoutLocation,
	String oldRelationship
	) 
	{
		assert (null != repositoryURL);
		assert (null != hostBranch);
		assert (null != mergeBranch);
		
		String executablePath = GetExecutablePath();

		Output output;
		
		// Add the merge branch as a remote to the source branch repository
		try {
			String[] myArgs = { "remote", "add", "-f", mergeBranch, mergeBranchSourceCheckoutLocation };
			output = RunIt.execute (executablePath, myArgs, hostBranchSourceCheckoutLocation, false);
		} catch (IOException e2) {
			return Constants.Relationship.Error.toString() + " Couldn't checkout host branch: " + e2.getMessage();
		}
		
		// TODO: check for history?
		
		// Perform merge
		try {
			String[] myArgs = { "merge", mergeBranch + "/" + mergeBranch };
			output = RunIt.execute (executablePath, myArgs, hostBranchSourceCheckoutLocation, false);
		} catch (IOException e2) {
			return Relationship.Error.toString() + " Couldn't perform a branch merge: " + e2.getMessage();
		}
		// Check for merge status
		if (output.getOutput().contains("CONFLICT"))
			return Relationship.MergeConflict.toString();
			
		return Relationship.MergeClean.toString();
	}
	
	public static String ResetHostBranchSourceRepository
	(
	String hostBranch,
	String hostBranchSourceCheckoutLocation
	)
	{
		Output output;
		// Reset
		
		String executablePath = GetExecutablePath();
		
		try {
			String[] myArgs = { "reset", "origin/" + hostBranch, "--hard" };
			output = RunIt.execute (executablePath, myArgs, hostBranchSourceCheckoutLocation, false);
		} catch (IOException e2) {
			return Relationship.Error.toString() + " Couldn't perform a branch merge: " + e2.getMessage();
		}
		
		return output.getOutput();
	}
	/**
	 * @param String
	 *            pathExecutable: the path to the executable
	 * @param String
	 *            pathToLocalRepo: the path to the local repo which this method creates
	 * @param String
	 *            tempWorkPath: path to a temp directory
	 * @return
	 * @effect: performs a pull and update on the pathToLocalRepo repository
	 */
	protected static synchronized void updateLocalRepositoryBranch
	(
	String pathToRemoteRepo, 
	String pathToLocalRepo, 
	String branch
	) throws IOException, OperationException 
		{
		assert (null != pathToLocalRepo);
		assert (null != pathToRemoteRepo);
		assert (null != branch);
		
		String pathExecutable = GetExecutablePath();
		
		// _log.info("update local repository");

		String command = pathExecutable + " pull "/*-u " + pathToRemoteRepo*/;
		List<String> myArgsList = new ArrayList<String>();
		myArgsList.add("pull");
		//myArgsList.add("-u");
		//myArgsList.add(pathToRemoteRepo);
		// TODO: make sure the correct branch is being pulled
		/*if (branch != null) {
			myArgsList.add("-b");
			myArgsList.add(branch);
			command += " -b " + branch;
		}*/

		// String[] myArgs = { "pull", "-u" };
		Output output = RunIt.execute(pathExecutable, myArgsList.toArray(new String[0]), pathToLocalRepo, false);
		if (pathExecutable.contains("git")) {
			/*_log.info("update local repository");
			_log.info("run command: " + command);
			_log.info("output: \n" + output.getOutput());
			if (output.getError().length() > 0) {
				_log.info("error: " + output.getError()); TODO: Logging
			}*/
		}

		// for git
		if (pathExecutable.contains("git") && (output.getOutput().indexOf("Fast-forward") < 0) && (output.getOutput().indexOf("Already up-to-date.") < 0)) {
			throw new OperationException(command, pathToLocalRepo, output.toString());
		}

		/*
		 * if (pathExecutable.contains("git")) { _log.info("output: \n" + output.getOutput() + "\n error: \n" + output.getError()); }
		 */
	}

	/**
	 * Pulls into the local repo and checks for an error in the cache.
	 * 
	 * @param ds
	 *            : the repo to pull into
	 * @param Git
	 *            : path to git executable
	 * @param localRepo
	 *            : path to the local copy of the repo
	 * @param tempWorkPath
	 *            : the temp path
	 * @param remoteGit
	 *            : the optional remoteGit command (null if none)
	 * @param repoName
	 *            : the name of the repo
	 * @param projectName
	 *            : the name of the project
	 * @throws GitOperationException
	 * @throws IOException
	 */
	public static synchronized void updateLocalRepositoryBranchAndCheckCacheError
	(
	String repositoryURL,
	String localRepo,
	String branch
	) throws OperationException, IOException 
	{
		// TODO Logging 
		//_log.info("update local repository and check cache error");
		//_log.info("call update local repository and check cache error");
		//_log.info("local repository: " + localRepo + ", exist?: " + (new File(localRepo).exists()));
		if (new File(localRepo).exists()) {
			/*
			try {
				//_log.info("trying to update local repository: " + localRepo);
				updateLocalRepositoryBranch(repositoryURL,  localRepo, branch);
				//_log.info("successfully finished updating local repository: " + localRepo);
			} catch (OperationException e) {
				//_log.info("operation exception in running update local repository");
				//_log.info("command: " + e.getCommand() + "\n path: " + e.getPath() + "\n output: " + e.getOutput() + "\n");
				String errorMsg = "Crystal is having trouble executing\n" + e.getCommand() + "\nin " + e.getPath() + ".\n" + "Crystal got the unexpected output:\n" + e.getOutput() + "\n";
				//_log.error(errorMsg);
				errorMsg += "Sometimes, clearing Crystal's local cache can remedy this problem";
				throw new OperationException(errorMsg, e.getPath(), e.getOutput());
				*/
			return;
			}
		else 
		{
			//_log.info("trying to create local repository: " + localRepo);
			createLocalRepository(repositoryURL, localRepo, branch);
			//_log.info("finished creating local repository: " + localRepo);
		}
	}
	
	/**
	 * @param String
	 *            pathToGit: the path to the git executable
	 * @param String
	 *            pathToRemoteRepo: the full path to the remote repo
	 * @param String
	 *            pathToLocalRepo: the path to the local repo which this method creates
	 * @param String
	 *            tempWorkPath: path to a temp directory
	 * @effect: clones the pathToRemoteRepo repository to pathToLocalRepo
	 */
	public static synchronized void createLocalRepository
	(
	String pathToRemoteRepo,
	String pathToLocalRepo,
	String branch/*,
	String remoteCmd*/ // TODO: is this needed?
	) throws IOException, OperationException 
	{
		assert (null != pathToRemoteRepo);
		assert (null != pathToLocalRepo);
		assert (null != branch);

		// _log.info("create local repository");
		// String git = prefs.getClientPreferences().getGitPath();

		// Create Directory
		File directory = new File(pathToLocalRepo); 
		if (!directory.exists())
			{
			boolean created = directory.mkdirs();
			if (created);
			}
		else
			return;
		
		String pathExecutable = GetExecutablePath();
		
		String command = pathExecutable + " clone";

		List<String> myArgsList = new ArrayList<String>();
		myArgsList.add("clone");
		/*if (remoteCmd != null) {
			myArgsList.add("--remotecmd");
			myArgsList.add(remoteCmd);
			command += " --remotecmd " + remoteCmd;
		}*/
		myArgsList.add(pathToRemoteRepo);
		if (branch != null)
		{
			myArgsList.add("-b");
			myArgsList.add(branch);
		}
		
		myArgsList.add(pathToLocalRepo);

		if (command != null)
			{
			command +=  " " + pathToRemoteRepo;
			if (branch != null)
				command +=  " -b " + branch;
			command +=  " " + pathToLocalRepo;
			}
		
		Output output = RunIt.execute(pathExecutable, myArgsList.toArray(new String[0]), pathToLocalRepo, true);

			/*_log.info("create local repository");
			_log.info("run command: " + command);
			_log.info("output: " + output.getOutput());
			if (output.getError().length() > 0) {
				_log.info("error: " + output.getError());
			}*/ // TODO: logging

		if ((!output.getError().isEmpty() || output.getStatus()!= 0) && output.getOutput().indexOf("updating to branch") < 0 && output.getOutput().indexOf("done.") < 0) {
			String errorMsg = "Crystal tried to execute command:\n" + "\"" + pathExecutable + " clone " + pathToRemoteRepo + " " + pathToLocalRepo + "\"\n" + "from \""
					+ pathToLocalRepo + "\"\n" + "but got the unexpected output:\n" + output.toString();
			// JOptionPane.showMessageDialog(null, dialogMsg, "git clone failure", JOptionPane.ERROR_MESSAGE);
			throw new OperationException(errorMsg, pathToLocalRepo, output.toString());
			// throw new RuntimeException("Could not clone repository " + pathToRemoteRepo + " to " + pathToLocalRepo + "\n" + output);
		}
	}
	
	public static synchronized String getParent(String pathToLocalRepo, String hostBranchName, String targetBranchName) throws IOException, OperationException
	{
		String pathExecutable = GetExecutablePath();
		
		List<String> myArgsList = new ArrayList<String>();
		myArgsList.add("merge-base");
		/*if (remoteCmd != null) {
			myArgsList.add("--remotecmd");
			myArgsList.add(remoteCmd);
			command += " --remotecmd " + remoteCmd;
		}*/
		
		myArgsList.add(hostBranchName);
		myArgsList.add("remotes/origin/" + targetBranchName);

		String command = pathExecutable + " merge-base";
		
		command +=  " " + hostBranchName;
		command +=  " remotes/origin/" + pathToLocalRepo;
		
		Output output = RunIt.execute(pathExecutable, myArgsList.toArray(new String[0]), pathToLocalRepo, true);
		
		if (!output.getError().isEmpty())
		{
			String errorMsg = "Execution of command failed:\n" + "\"" + command + "\" \n The error" + output.getError();
			throw new OperationException(errorMsg, pathToLocalRepo, output.toString());
		}
		
		String returnVal = output.getOutput();
		
		int newLineCharIndex = returnVal.lastIndexOf("\n");
		if (-1 != newLineCharIndex)
			returnVal = returnVal.substring(0, newLineCharIndex);
		
		return returnVal;
	}
	
	
	public static boolean cloneFromLocalRepository(String pathToLocalRepo, String pathToNewClone) throws OperationException, IOException
	{
		File directory = new File(pathToNewClone); 
		if (directory.exists())
			return false;
		
		String pathExecutable = GetExecutablePath();
		
		List<String> myArgsList = new ArrayList<String>();
		myArgsList.add("clone");
		myArgsList.add(pathToLocalRepo);
		myArgsList.add(pathToNewClone);
		
		Output output = RunIt.execute(pathExecutable, myArgsList.toArray(new String[0]), pathToLocalRepo, true);
		
		String command = pathExecutable + " clone";
		command +=  pathToLocalRepo + " " + pathToNewClone;
		
		if (!output.getError().isEmpty())
		{
			String errorMsg = "Execution of command failed:\n" + "\"" + command + "\" \n The error" + output.getError();
			throw new OperationException(errorMsg, pathToLocalRepo, output.toString());
		}
		
		return true;
	}

	
	public static void checkoutChangeSet(String pathToLocalRepo, String changeSetsha1) throws IOException, OperationException
	{		
		String pathExecutable = GetExecutablePath();
		
		List<String> myArgsList = new ArrayList<String>();
		myArgsList.add("checkout");
		myArgsList.add(changeSetsha1);
		
		Output output = RunIt.execute(pathExecutable, myArgsList.toArray(new String[0]), pathToLocalRepo, true);
		
		String command = pathExecutable + " checkout";
		command +=  changeSetsha1;
		
		if (!output.getError().isEmpty() && output.getStatus()!= 0)
		{
			String errorMsg = "Execution of command failed:\n" + "\"" + command + "\" \n The error" + output.getError();
			throw new OperationException(errorMsg, pathToLocalRepo, output.toString());
		}
		
	}
	
}
