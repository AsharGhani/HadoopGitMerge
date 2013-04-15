package org.gitreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;


class GitInputSplit extends InputSplit implements org.apache.hadoop.mapred.InputSplit{ 
	public long		m_maxSize;
	public long 	m_totalSize;
	public List<MergeInfo> m_mergeInfoList;
	public List<String> m_nodeLocations; 
	
	public GitInputSplit ()
	{
		m_maxSize = 0;
		m_mergeInfoList = new ArrayList<MergeInfo>();
		m_nodeLocations = new ArrayList<String>();
	}
	
	public GitInputSplit (long maximumSize)
	{
		m_maxSize = maximumSize;
		m_mergeInfoList = new ArrayList<MergeInfo>();
		m_nodeLocations = new ArrayList<String>();
	}
	
	@Override
	public long getLength() throws IOException {
		return m_totalSize;
	}

	@Override
	public String[] getLocations() throws IOException {
		// TODO Auto-generated method stub
		String[] locations = new String[m_nodeLocations.size()];
		
		locations = m_nodeLocations.toArray(locations);
		
		return locations;
		
	}
	
	public void AddMergeInfo(String nodeName, String hostBranchFile,
			String mergeBranchFile, String parentBranchFile, Long size) 
	{
		MergeInfo newMergeInfo = new MergeInfo(hostBranchFile, mergeBranchFile, parentBranchFile, size);
		m_mergeInfoList.add(newMergeInfo);
		
		boolean addNewLocation = true;
		for (String currlocation : m_nodeLocations)
		{
			if (currlocation.equals(nodeName))
				addNewLocation = false;
		}
		
		if (addNewLocation)
			m_nodeLocations.add(nodeName);
		
		m_totalSize += size;
	}
	

	@Override
	public void readFields(DataInput in) throws IOException 
	{
		m_maxSize = in.readLong();
		m_totalSize = in.readLong();
		
		int mergeInfoLength = in.readInt();		
		m_mergeInfoList = new ArrayList<MergeInfo>();
		for (int i=0; i<mergeInfoLength; i++)
		{
			String hostBranchFile = Text.readString(in);
			String mergeBranchFile = Text.readString(in);
			String parentBranchFile = Text.readString(in);
			long   size = in.readLong();
			
			m_mergeInfoList.add(new MergeInfo(hostBranchFile, mergeBranchFile, parentBranchFile, size));
		}
		
		int locationsLength = in.readInt();
		m_nodeLocations = new ArrayList<String>();
		for(int i=0; i<locationsLength;i++) 
		{
			m_nodeLocations.add(Text.readString(in));
		}		
	}
	
	@Override
	public void write(DataOutput out) throws IOException 
	{
		out.writeLong(m_maxSize);
		out.writeLong(m_totalSize);
		
		out.writeInt (m_mergeInfoList.size());	
		for (MergeInfo mergeInfo : m_mergeInfoList) 
		{
			Text.writeString (out, mergeInfo.m_hostBranchFile);
			Text.writeString (out, mergeInfo.m_mergeBranchFile);
			Text.writeString (out, mergeInfo.m_parentBranchFile);
			out.writeLong (mergeInfo.m_size);
		}		

		out.writeInt (m_nodeLocations.size());
		for(String location : m_nodeLocations) 
		{
			Text.writeString (out, location);
		}
	}
	
	class MergeInfo
	{
		public String m_hostBranchFile;
		public String m_mergeBranchFile;
		public String m_parentBranchFile;
		public long	  m_size;
		
		MergeInfo(String hostBranchName, String mergeBranchName, String parentBranchName, long size)
		{
			m_hostBranchFile = hostBranchName;
			m_mergeBranchFile = mergeBranchName;
			m_parentBranchFile =  parentBranchName;
			m_size = size;
		}
	}

}
