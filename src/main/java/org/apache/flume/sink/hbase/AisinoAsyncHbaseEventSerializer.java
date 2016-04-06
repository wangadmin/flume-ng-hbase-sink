package org.apache.flume.sink.hbase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.hbase.async.AtomicIncrementRequest;
import org.hbase.async.PutRequest;
import org.apache.flume.conf.ComponentConfiguration;
import org.apache.flume.sink.hbase.SimpleHbaseEventSerializer.KeyType;

import com.google.common.base.Charsets;

public class AisinoAsyncHbaseEventSerializer implements AsyncHbaseEventSerializer {
  private byte[] table;
  private byte[] cf;
  private byte[][] payload;
  private byte[][] payloadColumn;
  private final String payloadColumnSplit = ",";
  private byte[] incrementColumn;
  private String rowSuffix;
  private String rowSuffixCol;
  private byte[] incrementRow;
  private KeyType keyType;

  @Override
  public void initialize(byte[] table, byte[] cf) {
    this.table = table;
    this.cf = cf;
  }

  @Override
  public List<PutRequest> getActions() {
    List<PutRequest> actions = new ArrayList<PutRequest>();
    if(payloadColumn != null){
      byte[] rowKey;
      try {
    	System.out.println("<=====================keyType=====================>"+keyType);
        switch (keyType) {
          case TS:
            rowKey = SimpleRowKeyGenerator.getTimestampKey(rowSuffix);
            System.out.println("<=====================TS:rowKey=====================>"+rowKey);
            break;
          case TSNANO:
            rowKey = SimpleRowKeyGenerator.getNanoTimestampKey(rowSuffix);
            System.out.println("<=====================TSNANO:rowKey=====================>"+rowKey);
            break;
          case RANDOM:
            rowKey = SimpleRowKeyGenerator.getRandomKey(rowSuffix);
            System.out.println("<=====================RANDOM:rowKey=====================>"+rowKey);
            break;
          case CUSTOM:
            rowKey = SimpleRowKeyGenerator.getCustomKey(rowSuffix);
            System.out.println("<=====================CUSTOM:rowKey=====================>"+rowKey);
            break;
          default:
            rowKey = SimpleRowKeyGenerator.getUUIDKey(rowSuffix);
            System.out.println("<=====================default:rowKey=====================>"+rowKey);
            break;
        }
        System.out.println("break out switch out and go on................................");
        System.out.println("this.payload.length---------------->"+this.payload.length);//todo@!!@@@@
	for (int i = 0; i < this.payload.length; i++)
	{
			System.out.println("put to hbase's context----->:"+
					table+"--<--table" +rowKey+"--<--rowKey"+cf+"--<--cf"
			+payloadColumn[i]+"--<--payloadColumn[i]"+ payload[i]+"--<--payload[i]");
			PutRequest putRequest =  new PutRequest(table, rowKey, cf,payloadColumn[i], payload[i]);
        	actions.add(putRequest);
	}

      } catch (Exception e){
        throw new FlumeException("Could not get row key!", e);
      }
    }
    return actions;
  }

  public List<AtomicIncrementRequest> getIncrements(){
    List<AtomicIncrementRequest> actions = new
        ArrayList<AtomicIncrementRequest>();
    if(incrementColumn != null) {
      AtomicIncrementRequest inc = new AtomicIncrementRequest(table,
          incrementRow, cf, incrementColumn);
      //actions.add(inc);
    }
    return actions;
  }

  @Override
  public void cleanUp() {
    // TODO Auto-generated method stub

  }

  @Override
  public void configure(Context context) {
    String pCol = context.getString("payloadColumn", "pCol");
    String iCol = context.getString("incrementColumn", "iCol");
    rowSuffixCol = context.getString("rowPrefixCol", "rowkey");//
    String suffix = context.getString("suffix", "uuid");
    System.out.println("<=====================pCol=====================>"+pCol);
    System.out.println("<=====================iCol=====================>"+iCol);
    System.out.println("<=====================rowSuffixCol=====================>"+rowSuffixCol);
    System.out.println("<=====================suffix=====================>"+suffix);
    if(pCol != null && !pCol.isEmpty()) {
      if(suffix.equals("timestamp")){
        keyType = KeyType.TS;
        System.out.println("--------------->COME IN TIMESTAMP");
      } else if (suffix.equals("random")) {
        keyType = KeyType.RANDOM;
        System.out.println("--------------->COME IN RANDOM");
      } else if(suffix.equals("nano")){
        keyType = KeyType.TSNANO;
        System.out.println("--------------->COME IN NANO");
      }else if(suffix.equals("custom")){
    	keyType = KeyType.CUSTOM;
        System.out.println("--------------->COME IN CUSTOM");
      }else {
        keyType = KeyType.UUID;
        System.out.println("--------------->COME IN UUID");
      }   
     	String[] pCols = pCol.replace(" ", "").split(",");
     	payloadColumn = new byte[pCols.length][];
     	for (int i = 0; i < pCols.length; i++)
	{
		System.out.println("pCols[i]"+pCols[i]);
		payloadColumn[i] = pCols[i].toLowerCase().getBytes(Charsets.UTF_8);
	}
    }

    if(iCol != null && !iCol.isEmpty()) {
      incrementColumn = iCol.getBytes(Charsets.UTF_8);
    }
    incrementRow =
        context.getString("incrementRow", "incRow").getBytes(Charsets.UTF_8);
  }

  @Override
  public void setEvent(Event event) {
	String strBody = new String(event.getBody());
	System.out.println("strBody"+strBody);
	System.out.println(payloadColumnSplit+"payloadColumnSplit");
	String[] subBody = strBody.split(this.payloadColumnSplit);
	if (subBody.length == this.payloadColumn.length)
	{
		this.payload = new byte[subBody.length][];
		for (int i = 0; i < subBody.length; i++)
		{
			this.payload[i] = subBody[i].getBytes(Charsets.UTF_8);
			if ((new String(this.payloadColumn[i]).equals(this.rowSuffixCol)))
			{
				this.rowSuffix = subBody[i];
				System.out.println("------------------------------------>>>>>>>>>>>>>rowSuffix:"+subBody[i]);
			}
		}
	}
  }

  @Override
  public void configure(ComponentConfiguration conf) {
    // TODO Auto-generated method stub
  }


}