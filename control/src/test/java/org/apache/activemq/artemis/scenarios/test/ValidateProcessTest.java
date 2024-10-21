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

package org.apache.activemq.artemis.scenarios.test;

import java.io.File;

import org.apache.activemq.artemis.cli.commands.helper.HelperCreate;
import org.apache.activemq.artemis.scenarios.model.requests.BusinessProcessRequest;
import org.apache.activemq.artemis.scenarios.model.requests.OrdersIncomeRequest;
import org.apache.activemq.artemis.scenarios.service.BusinessService;
import org.apache.activemq.artemis.scenarios.service.IncomeService;
import org.apache.activemq.artemis.util.ServerUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ValidateProcessTest {


   private static final String HOME_LOCATION = "./target/artemis-release/apache-artemis-2.39.0-SNAPSHOT";
   private static final String SERVER_NAME = "myServer";
   private static final String ARTEMIS_INSTANCE = "./target/" + SERVER_NAME;

   private Process serverProcess;

   private String QUEUE_LIST = "IncomeOrder,Manufacturing,Manufacturing.Line0,Manufacturing.Line1,Manufacturing.Line2,Manufacturing.Line3,Manufacturing.Line4,Manufacturing.Line5,Manufacturing.Line6,Manufacturing.Line7,Manufacturing.Line8,Manufacturing.Line9,Manufacturing.Line10";

   @BeforeAll
   public static void createServer() throws Exception {
      {
         HelperCreate cliCreateServer = new HelperCreate(new File(HOME_LOCATION));
         cliCreateServer.setArtemisInstance(new File(ARTEMIS_INSTANCE));
         cliCreateServer.addArgs("--queues", "queueTest,Div,Div.0,Div.1,Div.2");
         cliCreateServer.createServer();
      }
   }

   @BeforeEach
   public void startProcess() throws Exception {
      serverProcess = ServerUtil.startServer("./target/myServer", "myServer");
      ServerUtil.waitForServerToStart(0, 5000);

   }

   @AfterEach
   public void killProcess() throws Throwable {
      serverProcess.destroyForcibly();
      serverProcess.waitFor();
   }

   @Test
   public void testProcess() throws Exception {
      OrdersIncomeRequest ordersIncomeRequest = new OrdersIncomeRequest();
      ordersIncomeRequest.setNumberOfOrders(1000).setCommitInterval(100).setUri("tcp://localhost:61616").setProtocol("CORE");

      IncomeService incomeService = new IncomeService();
      incomeService.process(ordersIncomeRequest);

      BusinessProcessRequest businessProcessRequest = new BusinessProcessRequest();
      businessProcessRequest.setUri("tcp://localhost:61616").setProtocol("CORE");

      businessProcessRequest.setElements(1000).setConnections(10);

      BusinessService businessService = new BusinessService();
      businessService.process(businessProcessRequest);
   }
}
