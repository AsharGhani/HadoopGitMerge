package Utils;

/**
 * An exception for GitOperations
 */
public class OperationException extends Exception {
	private static final long serialVersionUID = -6885233021486785003L;

	private String _output;
	private String _command;
	private String _path;

	/**
	 * Creates a new GitOperationException
	 * 
	 * @param command
	 *            : the git command that caused the exception.
	 * @param path
	 *            : the path from which the command was run.
	 * @param output
	 *            : the output of the command.
	 */
	public OperationException(String command, String path, String output) {
		super("Tried to execute \n\"" + command + "\"\n in \"" + path + "\"\n" + "but got the output\n" + output);
		_output = output;
		_path = path;
		_command = command;
	}

	/**
	 * @return the output of the command.
	 */
	public String getOutput() {
		return _output;
	}

	/**
	 * @return the path in which this command was run.
	 */
	public String getPath() {
		return _path;
	}

	/**
	 * @return the command that was run.
	 */
	public String getCommand() {
		return _command;
	}

}