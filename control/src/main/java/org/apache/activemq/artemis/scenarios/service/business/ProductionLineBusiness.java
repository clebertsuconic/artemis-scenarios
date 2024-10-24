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

package org.apache.activemq.artemis.scenarios.service.business;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.artemis.scenarios.service.BaseListener;

public class ProductionLineBusiness {

   public static final String OUTPUT_ADDRESS = "ProductionCompletion";
   public static final String INCOME_ADDRESS = "Manufacturing.Line";

   public static void configureListener(Connection connection) throws Exception {
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

      for (int i = 0; i < 10; i++) {
         MessageConsumer consumer = session.createConsumer(session.createQueue(INCOME_ADDRESS + i));
         Listener listener = new Listener(session, consumer);
         consumer.setMessageListener(listener);
         connection.start();
      }
   }


   private static class Listener extends BaseListener {

      Listener(Session session, MessageConsumer consumer) throws Exception {
         super(session, consumer);
      }

      protected MessageProducer createProducer() throws Exception {
         return session.createProducer(session.createQueue(OUTPUT_ADDRESS));
      }
   }

}
