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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.artemis.scenarios.model.requests.BusinessProcessRequest;
import org.apache.activemq.artemis.scenarios.model.response.BaseResponse;

public class BusinessService extends BaseService {

   public BaseResponse process(BusinessProcessRequest bpRequest) throws Exception {

      ConnectionFactory cf = createConnectionFactory(bpRequest.getProtocol(), bpRequest.getUri());

      try (Connection connection = cf.createConnection(bpRequest.getUser(), bpRequest.getPassword())) {

         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         MessageConsumer consumer = session.createConsumer(session.createQueue("Orders"));
         MessageProducer producer = session.createProducer(session.createQueue("Production"));
         connection.start();
         for (int i = 0; i < bpRequest.getElements(); i++) {
            Message message = consumer.receive(5000);
            if (message != null) {
               producer.send(message);
            }
         }

         session.commit();
      }

      return new BaseResponse().setOk(true);
   }

}
