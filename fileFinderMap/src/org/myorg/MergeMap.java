package org.myorg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.hdfs.web.resources.RecursiveParam;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;


public class MergeMap {
	
	private ArrayList<FileStatus> fstat = new ArrayList<FileStatus>();
	
	public ArrayList<FileStatus> getStatus(){
		return (ArrayList<FileStatus>)fstat.clone();
	}
	
	public void setStatus(ArrayList<FileStatus> stat){
		fstat = (ArrayList<FileStatus>)stat.clone();
	}
	
	
	//We use these for very fast accesses to specific fstat data
	private ArrayList<String> pathNames = new ArrayList<String>();
	private ArrayList<Long> fileSizes = new ArrayList<Long>();
	
	public ArrayList<String> getPathNames(){
		return (ArrayList<String>)pathNames.clone();
	}
	
	public void setPathNames(ArrayList<String> pn){
		pathNames = (ArrayList<String>)pn.clone();
	}
	
	public ArrayList<Long> getFileSizes(){
		return (ArrayList<Long>)fileSizes.clone();
	}
	
	public void setFileSizes(ArrayList<Long> fz){
		fileSizes = (ArrayList<Long>)fz.clone();
	}
	
	
	private FileStatus[] shellListStatus(String cmd, FileSystem srcFs, FileStatus src) {
		if (!src.isDir()) {
				      FileStatus[] files = { src };
				      return files;
				    }
				    Path path = src.getPath();
				    try {
				      FileStatus[] files = srcFs.listStatus(path);
				      if ( files == null ) {
				        System.err.println(cmd + 
				                           ": could not get listing for '" + path + "'");
				      }
				      return files;
				    } 
				    catch (IOException e) {
				      System.err.println(cmd + 
				                         ": could not get get listing for '" + path + "' : " +
				                         e.getMessage().split("\n")[0]);
				    }
				    return null;
	}
		
	
	public void recursive_ls(String baseDir) throws IOException{
		Path baseDirPath = new Path(baseDir);
		FileSystem fs = FileSystem.get(new Configuration());
		FileStatus src = fs.getFileStatus(baseDirPath);
		ls(src, fs, true);
	}
	
	public void ls(FileStatus src, FileSystem srcFs, boolean recursive){
		final String cmd = recursive? "lsr": "ls";
		final FileStatus[] items = shellListStatus(cmd, srcFs, src);
		for (int i = 0; i < items.length; i ++){
			FileStatus stat = items[i];
			updateState(stat, true, true);
			if (recursive && stat.isDir()){
				ls(stat,srcFs, true);
			}
		}
	}
	
	public void updateState(FileStatus stat, boolean ommitDirectory, 
			boolean fasterImplementation){
		
		if (stat.isDir() && ommitDirectory) return;
		ArrayList<FileStatus> tempStat =getStatus();
		tempStat.add(stat);
		setStatus(tempStat);
		
		if (fasterImplementation){
			ArrayList<String> tempPathNames = getPathNames();	
			tempPathNames.add(stat.getPath().toUri().getPath());
			setPathNames(tempPathNames);
			
			ArrayList<Long> tempFileSizes = getFileSizes();	
			tempFileSizes.add(stat.getLen());
			setFileSizes(tempFileSizes);
		}
	}
	
	public void writeStat(FileStatus stat){
		ArrayList<FileStatus> tempStatus = getStatus();
		for (int i = 0; i < tempStatus.size(); i ++){
			System.out.printf("%"+ 10 + "d ", stat.getLen());
			System.out.println(stat.getPath().toUri().getPath());	
		}
	}
	
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, LongWritable> {
	//    private Text branchFiles = new Text();
//	    private Text fileSizes = new Text(); 
	    
	    private String hostBranchDir, mergeBranchDir, parentBranchDir;

		public void map(LongWritable key, Text value, OutputCollector<Text, LongWritable> output, Reporter reporter) throws IOException {
	        String line = value.toString();
	        StringTokenizer tokenizer = new StringTokenizer(line);
	        MergeMap mmapHost = new MergeMap();
	        MergeMap mmapMerge = new MergeMap();
	        MergeMap mmapParent = new MergeMap();
	        
	        /* Assume a properly formatted merge input
	        ** Input file is of form
	        ** <HostBranchDir, mergeBranchDir,parentBranchDir>
	        ** Output is of form
	        ** <HostBranchFile, mergeBranchFile,parentBranchFile, maxFileSize>
	        */
	        
	        hostBranchDir = tokenizer.nextToken();
	        mergeBranchDir = tokenizer.nextToken();
	        parentBranchDir = tokenizer.nextToken();
	        
	        //Obtain the required file statuses
	        mmapHost.recursive_ls(hostBranchDir);
	        mmapMerge.recursive_ls(mergeBranchDir);
	        mmapParent.recursive_ls(parentBranchDir);
	        
	        //find the common files
	        ArrayList<String> hostPathNames = mmapHost.getPathNames();
	        ArrayList<String> mergePathNames = mmapMerge.getPathNames();
	        ArrayList<String> parentPathNames = mmapParent.getPathNames();
	        
	        ArrayList<Long> hostFileSizes = mmapHost.getFileSizes();
	        ArrayList<Long> mergeFileSizes = mmapMerge.getFileSizes();
	        ArrayList<Long> parentFileSizes = mmapParent.getFileSizes();
	        
	        for (int i=0; i < hostPathNames.size(); i ++){
	        	String currHostPathName = hostPathNames.get(i);
	        	String predictedMergePathName = currHostPathName.replace(hostBranchDir, mergeBranchDir);
	        	String predictedParentPathName = currHostPathName.replace(hostBranchDir, parentBranchDir);
	        	
	        	int mergeBranchIndex = predictedMergePathName.indexOf(predictedMergePathName);
	        	int parentBranchIndex = predictedParentPathName.indexOf(predictedParentPathName);
	        	
	        	Long currHostFileSize = hostFileSizes.get(i);
	        	Long currMergeFileSize = mergeFileSizes.get(mergeBranchIndex);
	        	Long currParentFileSize = parentFileSizes.get(parentBranchIndex);
	        	
	        	if (mergePathNames.contains(predictedMergePathName)){
	        		if (parentPathNames.contains(predictedParentPathName)){
	        			output.collect(new Text(currHostPathName + " " + predictedMergePathName + 
	        					" " + predictedParentPathName), 
	        					new LongWritable(Math.max(currHostFileSize, Math.max(currMergeFileSize
	        							, currParentFileSize))) );
		        		parentPathNames.remove(parentBranchIndex);
		        		parentFileSizes.remove(parentBranchIndex);
	        		}
	        		else{
	        			output.collect(new Text(currHostPathName + " " + predictedMergePathName + 
	        					" null"), 
	        					new LongWritable(Math.max(currHostFileSize, currMergeFileSize)) );
	        		}
        			mergePathNames.remove(mergeBranchIndex);
        			mergeFileSizes.remove(mergeBranchIndex);
	        	}
	        	else{
	        		if (parentPathNames.contains(predictedParentPathName)){
	        			output.collect(new Text(currHostPathName + " " + "null" + 
	        					" " + predictedParentPathName), 
	        					new LongWritable(Math.max(currHostFileSize, currParentFileSize)) );
		        		parentPathNames.remove(parentBranchIndex);
		        		parentFileSizes.remove(parentBranchIndex);
	        		}
	        		else{
	        			output.collect(new Text(currHostPathName + " null null"), 
	        					new LongWritable(hostFileSizes.get(i)) );
	        		}
	        	}
	        }
	        
	        for (int i = 0 ; i < mergePathNames.size(); i ++){
	        	String currMergePathName = hostPathNames.get(i);
	        	String predictedParentPathName = currMergePathName.replace(mergeBranchDir, parentBranchDir);
	        	
	        	int parentBranchIndex = predictedParentPathName.indexOf(predictedParentPathName);

	        	Long currMergeFileSize = mergeFileSizes.get(i);
	        	Long currParentFileSize = parentFileSizes.get(parentBranchIndex);

	        	if (parentPathNames.contains(predictedParentPathName)){
        			output.collect(new Text("null " + currMergePathName + 
        					" " + predictedParentPathName), 
        					new LongWritable(Math.max(currMergeFileSize, currParentFileSize)) );
	        		parentPathNames.remove(parentBranchIndex);
	        		parentFileSizes.remove(parentBranchIndex);
        		}
        		else{
        			output.collect(new Text("null " + currMergePathName + " null"), 
        					new LongWritable(currMergeFileSize) );
        		}
	        }
	        
	        for (int i = 0 ; i < mergePathNames.size(); i ++){
	        	output.collect(new Text("null null " + parentPathNames.get(i)), 
        					new LongWritable(parentFileSizes.get(i)) );
	        }
	        
		}
	}
	
	public static void main(String [] args) throws IOException{
		
		  JobConf conf = new JobConf(MergeMap.class);
	      conf.setJobName("MergeMap1");

	      conf.setOutputKeyClass(Text.class);
	      conf.setOutputValueClass(IntWritable.class);

	      conf.setMapperClass(Map.class);
	      conf.setMapOutputValueClass(LongWritable.class);
	      conf.setMapOutputKeyClass(Text.class);

	      conf.setInputFormat(TextInputFormat.class);
	      conf.setOutputFormat(TextOutputFormat.class);

	      FileInputFormat.setInputPaths(conf, new Path(args[0]));
	      FileOutputFormat.setOutputPath(conf, new Path(args[1]));

	      JobClient.runJob(conf);
	
	    //move this section to mapper	      
		//MergeMap1 mer = new MergeMap1();
		//mer.recursive_ls("/user/hamid_bably/gitinput/");

		//FileInputFormat.setInputPaths(conf, new Path(args[0]));
	     // FileOutputFormat.setOutputPath(conf, new Path(args[1]));
	}
}
