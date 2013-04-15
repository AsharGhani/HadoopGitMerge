package org.gitreduce;

import java.io.IOException;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.gitreduce.GitInputSplit.MergeInfo;

/**
 * RecordReader is responsible from extracting records from the InputSplit. 
 * This record reader accepts a {@link MultiFileSplit}, which encapsulates several 
 * files, and no file is divided.
 */
public class GitRecordReader implements RecordReader<LongWritable, Text> {

	private GitInputSplit 	m_split;
	private long 			m_sizereadSoFar;
	private int 			m_count = 0;

	public GitRecordReader(GitInputSplit split) throws IOException 
	{
		m_split = split;		
		m_count = 0;
		m_sizereadSoFar = 0;
	}
	
	@Override
	public boolean next(LongWritable key, Text value) throws IOException 
	{
		if(m_count >= m_split.m_mergeInfoList.size())
			return false;
		
		MergeInfo mergeInfo = m_split.m_mergeInfoList.get(m_count);
		
		key.set(m_count);
		value.set(mergeInfo.m_hostBranchFile + " " + mergeInfo.m_mergeBranchFile+ " " + mergeInfo.m_parentBranchFile );
		
		m_count++;
		m_sizereadSoFar += mergeInfo.m_size;
		
		return true;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public long getPos() throws IOException {
		return m_sizereadSoFar;
	}

	@Override
	public float getProgress() throws IOException {
		return ((float) m_sizereadSoFar) / m_split.m_totalSize;
	}


	@Override
	public LongWritable createKey() {
		// TODO Auto-generated method stub
		LongWritable key = new LongWritable();
		return key;
	}


	@Override
	public Text createValue() {
		// TODO Auto-generated method stub
		Text value = new Text();
		return value;
	}
}