package org.gitreduce;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.tools.MRAdmin;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;

/**
 * Can execute a three-way merge. This class is thread safe.
 *
 * @version $Id$
 */
public class GitReduce
{
	public static String readFile(String fileName) throws IOException{
		Path path = new Path(fileName);
		//FileSystem fs = path.getFileSystem(context.getConfiguration()); // context of mapper or reducer
		FileSystem fs = FileSystem.get(new Configuration());//path.getFileSystem(new Configuration()); // context of mapper or reducer
		FSDataInputStream fdsis = fs.open(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fdsis));
		String line = "";
		StringBuilder lines = new StringBuilder();
		//ArrayList<String> lines = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
		//    lines.add(line);
			lines.append(line + "\n");
		}
		br.close();
		return lines.toString();
		
	}
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
	      private String fileHostBranch, fileMergeBranch, fileParentBranch;
	      private String fileHostContents, fileMergeContents, fileParentContents;
	      public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
	    	  String line = value.toString();
	    	  StringTokenizer tk = new StringTokenizer(line);
	    	  while (tk.hasMoreTokens()){
	    		  fileHostBranch = tk.nextToken();
	    		  fileMergeBranch = tk.nextToken();
	    		  fileParentBranch = tk.nextToken();
	    		  
	    		  // TODO: handle null file case better
	    		  if (fileHostBranch.equals("null") || fileMergeBranch.equals("null") || fileParentBranch.equals("null"))
	    			  continue;
	    		  
	    		  String fileHostBranchDir = fileHostBranch.substring(0,fileHostBranch.lastIndexOf("/"));
	    		  String fileMergeBranchDir = fileMergeBranch.substring(0,fileMergeBranch.lastIndexOf("/"));
	    		  
	    		  fileHostContents = readFile(fileHostBranch);
	    		  fileMergeContents = readFile(fileMergeBranch);
	    		  fileParentContents = readFile(fileParentBranch);
	    	
	    		  //Input is of the form <fileHostBranch, fileMergeBranch, fileParentBranch>
		    	  //Output is of the form <fileHostBranch, fileMergeBranch, 0/1>
	    		  IntWritable conflict = new IntWritable(merge(fileHostContents, fileMergeContents, fileParentContents, Charset.forName("UTF-8")));
	    		  
	    		  output.collect(new Text(fileHostBranchDir + "-" + fileMergeBranchDir), conflict);  
	    	  }  
	      }
	}
	
	 public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
	      public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
	        int sum = 0;
	        while (values.hasNext()) {
	          sum += values.next().get();
	        }
	        output.collect(key, new IntWritable(sum));
	      }
	    }
	 
	  
    public static int merge(
                String base,
                String generated,
                String edited,
                Charset charset)
            throws IOException
    {
        MergeAlgorithm mergeAlgorithm = new MergeAlgorithm();
        MergeResult<RawText> mergeResult;
        mergeResult = mergeAlgorithm.merge(
		        RawTextComparator.DEFAULT,
		        new RawText(base.getBytes(charset)),
		        new RawText(generated.getBytes(charset)),
		        new RawText(edited.getBytes(charset)));
		return mergeResult.containsConflicts() == true ? 1 : 0;
    }
    
    public static void main(String args[]) throws IOException{
    	JobConf conf = new JobConf(GitReduce.class);
        conf.setJobName("GitReduce");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(IntWritable.class);

        conf.setMapperClass(Map.class);
       // conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(GitInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));
        JobClient.runJob(conf);
    }
    //	GitReduce m = new GitReduce();
    	//System.out.println(m.merge("1\n2\n3\n4", "1\n2\n3", "1\n2\n3", Charset.forName("UTF-8")));
    	//System.out.println(m.merge("12\n4", "1\n\n3", "1\n2\n3", Charset.forName("UTF-8")));
}