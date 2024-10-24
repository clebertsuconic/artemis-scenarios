/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.scenarios.service;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

public abstract class BaseListener implements MessageListener {

   protected final Session session;
   protected final MessageConsumer consumer;

   protected MessageProducer producer = null;


   @Override
   public void onMessage(Message message) {
      try {
         if (producer == null) {
            producer = createProducer();
         }

         message = processMessage(message);

         producer.send(message);
         session.commit();
      } catch (Exception e) {
         e.printStackTrace();
         try {
            session.rollback();
         } catch (Throwable e2) {
            e.printStackTrace();
         }
      }
   }
   protected abstract MessageProducer createProducer() throws Exception;

   public BaseListener(Session session, MessageConsumer consumer) throws Exception {
      this.session = session;
      this.consumer = consumer;
   }

   protected Message processMessage(Message message) {
      return message;
   }

}
