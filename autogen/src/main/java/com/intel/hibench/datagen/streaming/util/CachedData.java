/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.hibench.datagen.streaming.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cache the total records in memory.
 */
public class CachedData {

  private volatile static CachedData cachedData;

  private List<String> data;

  private int next;
  private int totalRecords;

  public static CachedData getInstance(String seedFile, long fileOffset, String dfsMaster) {
    if(cachedData == null) {
      synchronized (CachedData.class) {
        if (cachedData == null) {
          cachedData = new CachedData(seedFile, fileOffset, dfsMaster);
        }
      }
    }
    return cachedData;
  }

  private CachedData(String seedFile, long fileOffset, String dfsMaster){
    Configuration dfsConf = new Configuration();
    dfsConf.set("fs.default.name", dfsMaster);
    dfsConf.set("hadoop.security.authentication", "kerberos");
    dfsConf.set("dfs.namenode.kerberos.principal", "nn/_HOST@HDP.DSMAIN.DS.CORP");
    dfsConf.addResource("/etc/hadoop/conf/core-site.xml"); // Replace with actual path
    dfsConf.addResource("/etc/hadoop/conf/hdfs-site.xml"); // Replace with actual path

    UserGroupInformation.setConfiguration(dfsConf);
    // Subject is taken from current user context
    try{
       UserGroupInformation.loginUserFromSubject(null);
    }catch(IOException e){
       e.printStackTrace();
    }

    // read records from seedFile and cache into "data"
    data = new ArrayList<String>();
    BufferedReader reader = SourceFileReader.getReader(dfsConf, seedFile, fileOffset);
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        // System.out.println("read");
        data.add(line);
      }
    } catch (IOException e) {
      System.err.println("Failed read records from Path: " + seedFile);
      e.printStackTrace();
    }

    this.next = 0;
    this.totalRecords = data.size();
  }

  /**
   * Loop get record.
   * @return the record.
   */
  public String getRecord() {
    int current = next;
    next = (next + 1) % totalRecords;
    return data.get(current);
  }
}
