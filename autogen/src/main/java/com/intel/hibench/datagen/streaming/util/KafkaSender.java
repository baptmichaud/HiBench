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

import java.util.Random;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * KafkaSender hold an kafka producer. It gets content from input parameter, generates records and
 * sends records to kafka.
 */
public class KafkaSender {

  KafkaProducer kafkaProducer;
  CachedData cachedData;
  int recordLength;
  int intervalSpan;

  StringSerializer serializer = new StringSerializer();

  // Constructor
  public KafkaSender(String brokerList, String seedFile,
      long fileOffset, String dfsMaster, int recordLength, int intervalSpan) {

    // Details of KafkaProducerConfig could be find from:
    // http://kafka.apache.org/documentation.html#producerconfigs
    // Properties props = new Properties();
    //props.setProperty("sasl.kerberos.service.name", "kafka");    
    //props.setProperty("security.protocol", "SASL_PLAINTEXT");
    //props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
    //props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    //props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    //props.setProperty(ProducerConfig.ACKS_CONFIG, "1");
    //props.getProperty(ProducerConfig.CLIENT_ID_CONFIG, "DataGenerator");*/
      
    Properties props = new Properties();
    props.put("bootstrap.servers", brokerList);
    System.out.println("Servers:" + brokerList);

    // specify the protocol for SSL Encryption
    props.put("security.protocol", "SASL_PLAINTEXT");
    props.put("metadata.broker.list", brokerList);
    props.put("acks", "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    props.put("sasl.kerberos.service.name", "kafka");

    this.kafkaProducer = new KafkaProducer<String, String>(props);

    this.cachedData = CachedData.getInstance(seedFile, fileOffset, dfsMaster);
    this.recordLength = recordLength;
    this.intervalSpan = intervalSpan;
  }

  // The callback function will be triggered when receive ack from kafka.
  // Print error message if exception exist.
  Callback callback = new Callback() {
    public void onCompletion(RecordMetadata metadata, Exception e) {
           if (e != null) {
               System.out.println("Error while producing message to topic :" + metadata);
               e.printStackTrace();
           } else {
               String message = String.format("sent message to topic:%s partition:%s  offset:%s", metadata.topic(), metadata.partition(), metadata.offset());
               System.out.println(message);
           }
    }
  };

   private static class TestCallback implements Callback {
       @Override
       public void onCompletion(RecordMetadata recordMetadata, Exception e) {
           if (e != null) {
               System.out.println("Error while producing message to topic :" + recordMetadata);
               e.printStackTrace();
           } else {
               String message = String.format("sent message to topic:%s partition:%s  offset:%s", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
               System.out.println(message);
           }
       }
   }

  // send content to Kafka
  public long send (String topic, long targetRecords, boolean debugMode) {

    long sentRecords = 0L;
    long sentBytes = 0L;

    while (sentRecords < targetRecords) {
      String line = cachedData.getRecord();

        System.out.println(line);
       String currentTime = Long.toString(System.currentTimeMillis());

      // Key and Value will be serialized twice.
      // 1. in producer.send method
      // 2. explicitly serialize here to count byte size.
      byte[] keyByte = serializer.serialize(topic, currentTime);
      byte[] valueByte = fillArray(keyByte, serializer.serialize(topic, line));

      TestCallback callback = new TestCallback();
      //Random rnd = new Random();
      //for (long i = 0; i < 100 ; i++) {
      //   ProducerRecord<String, String> data = new ProducerRecord<String, String>(
      //           topic, "key-" + i, "message-"+i );
      //   kafkaProducer.send(data, callback);
      // }

      //kafkaProducer.close();

      ProducerRecord serializedRecord = new ProducerRecord(topic, keyByte, valueByte);
      //System.out.println(serializedRecord);
      kafkaProducer.send(serializedRecord, callback);

      //update counter
      sentRecords++;
      //System.out.println(sentRecords);
      sentBytes = sentBytes + keyByte.length + valueByte.length;
    }

    return sentRecords;
  }

  // Get byte array with fixed length (value length + key length = recordLength)
  private byte[] fillArray(byte[] key, byte[] line) {

    int valueLength = recordLength - key.length;
    byte[] valueByte;
    if (valueLength > 0) {
      valueByte = new byte[valueLength];
      if (line.length < valueLength) {
        // There is no enough content in line, fill rest space with 0
        System.arraycopy(line, 0, valueByte, 0, line.length);
        Arrays.fill(valueByte, line.length, valueLength, (byte)0);
      } else {
        System.arraycopy(line, 0, valueByte, 0, valueLength);
      }
    } else {
      // recordLength is smaller than the length of key, return empty array.
      valueByte = new byte[0];
    }
    return valueByte;
  }

  // close kafka producer
  public void close() {
    kafkaProducer.close();
  }
}
