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
import org.apache.activemq.artemis.utils.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateProcessTest {

   // Set this to false if you want to create and start your own server
   public static final boolean CREATE_SERVERS = false;

   private static final String DIVERT_CONFIGURATION = "RequiredDiverts.txt";

   private static final String HOME_LOCATION = "./target/artemis-release/apache-artemis-2.39.0-SNAPSHOT";
   private static final String SERVER_NAME = "myServer";
   private static final File ARTEMIS_INSTANCE = new File("./target/" + SERVER_NAME);

   private Process serverProcess;


   private static String QUEUE_LIST = "IncomeOrder,Manufacturing.Line0,Manufacturing.Line1,Manufacturing.Line2,Manufacturing.Line3,Manufacturing.Line4,Manufacturing.Line5,Manufacturing.Line6,Manufacturing.Line7,Manufacturing.Line8,Manufacturing.Line9,Manufacturing.Line10";
   private static String ADDRESS_LIST = "Delivery";

   @BeforeAll
   public static void createServer() throws Exception {
      if (!CREATE_SERVERS) {
         return;
      }
      {
         FileUtil.deleteDirectory(ARTEMIS_INSTANCE);
         HelperCreate cliCreateServer = new HelperCreate(new File(HOME_LOCATION));
         cliCreateServer.setArtemisInstance(ARTEMIS_INSTANCE);
         cliCreateServer.addArgs("--queues", QUEUE_LIST);
         cliCreateServer.addArgs("--addresses", ADDRESS_LIST);
         cliCreateServer.createServer();


         String divertConfig = FileUtil.readFile(ValidateProcessTest.class.getClassLoader().getResourceAsStream(DIVERT_CONFIGURATION));
         assertNotNull(divertConfig);
         File brokerXML = new File(ARTEMIS_INSTANCE, "/etc/broker.xml");
         assertTrue(FileUtil.findReplace(brokerXML, "</acceptors>", "</acceptors>\n" + divertConfig));
      }
   }

   @BeforeEach
   public void beforeTest() {
      if (!CREATE_SERVERS) {
         return;
      }
      File dataDirectory = new File(ARTEMIS_INSTANCE, "./data");
      FileUtil.deleteDirectory(dataDirectory);
   }

   @BeforeEach
   public void startProcess() throws Exception {
      if (CREATE_SERVERS) {
         serverProcess = ServerUtil.startServer("./target/myServer", "myServer");
         ServerUtil.waitForServerToStart(0, 5000);
      }
   }

   @AfterEach
   public void killProcess() throws Throwable {
      if (CREATE_SERVERS) {
         serverProcess.destroyForcibly();
         serverProcess.waitFor();
      }
   }

   @Test
   public void testProcess() throws Exception {
      OrdersIncomeRequest ordersIncomeRequest = new OrdersIncomeRequest();
      ordersIncomeRequest.setNumberOfOrders(1).setCommitInterval(100).setUri("tcp://localhost:61616").setProtocol("CORE");

      IncomeService incomeService = new IncomeService();
      incomeService.process(ordersIncomeRequest);

      BusinessProcessRequest businessProcessRequest = new BusinessProcessRequest();
      businessProcessRequest.setUri("tcp://localhost:61616").setProtocol("CORE");

      businessProcessRequest.setElements(1).setConnections(10);

      BusinessService businessService = new BusinessService();
      businessService.process(businessProcessRequest);
   }
}
