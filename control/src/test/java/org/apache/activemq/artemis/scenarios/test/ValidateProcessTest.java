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
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.management.SimpleManagement;
import org.apache.activemq.artemis.cli.commands.helper.HelperCreate;
import org.apache.activemq.artemis.scenarios.model.requests.BusinessProcessRequest;
import org.apache.activemq.artemis.scenarios.model.requests.OrdersIncomeRequest;
import org.apache.activemq.artemis.scenarios.service.BusinessService;
import org.apache.activemq.artemis.scenarios.service.IncomeService;
import org.apache.activemq.artemis.scenarios.service.business.DeliveryRouteBusiness;
import org.apache.activemq.artemis.scenarios.service.business.ManufactureRouterBusiness;
import org.apache.activemq.artemis.util.ServerUtil;
import org.apache.activemq.artemis.utils.FileUtil;
import org.apache.activemq.artemis.utils.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateProcessTest {

   // Set this to false if you want to create and start your own server
   public static final boolean CREATE_SERVERS = Boolean.parseBoolean(System.getProperty("ValidateProcessTest.createServers", "true"));

   public static final String ORDERS_INCOME_REQUEST_URI = System.getProperty("ValidateProcessTest.ordersIncomeRequest", "tcp://localhost:61616");
   public static final String BUSINESS_PROCESS_REQUEST_URI = System.getProperty("ValidateProcessTest.businessProcessRequest", "tcp://localhost:61616");
   public static final String SIMPLE_MANAGEMENT_URI = System.getProperty("ValidateProcessTest.simpleManagement", "tcp://localhost:61616");


   // Set this to true if you want to enable tracing on the server
   public static final boolean TRACE_LOGS = false;

   private static final String DIVERT_CONFIGURATION = "RequiredDiverts.txt";

   private static final String HOME_LOCATION = "./target/artemis-release/apache-artemis-2.39.0-SNAPSHOT";
   private static final String SERVER_NAME = "myServer";
   private static final File ARTEMIS_INSTANCE = new File("./target/" + SERVER_NAME);

   private Process serverProcess;

   private static String QUEUE_LIST = "ProductionCompletion,IncomeOrder,Manufacturing.Line0,Manufacturing.Line1,Manufacturing.Line2,Manufacturing.Line3,Manufacturing.Line4,Manufacturing.Line5,Manufacturing.Line6,Manufacturing.Line7,Manufacturing.Line8,Manufacturing.Line9";
   private static String ADDRESS_LIST = "DeliveryRoutes";

   @BeforeAll
   public static void createServer() throws Exception {
      if (!CREATE_SERVERS) {
         return;
      }
      FileUtil.deleteDirectory(ARTEMIS_INSTANCE);

      HelperCreate cliCreateServer = new HelperCreate(new File(HOME_LOCATION));
      cliCreateServer.setArtemisInstance(ARTEMIS_INSTANCE);
      cliCreateServer.addArgs("--queues", QUEUE_LIST);
      cliCreateServer.addArgs("--addresses", ADDRESS_LIST);
      cliCreateServer.setNoWeb(false);
      cliCreateServer.createServer();


      String divertConfig = FileUtil.readFile(ValidateProcessTest.class.getClassLoader().getResourceAsStream(DIVERT_CONFIGURATION));
      assertNotNull(divertConfig);
      File brokerXML = new File(ARTEMIS_INSTANCE, "/etc/broker.xml");
      assertTrue(FileUtil.findReplace(brokerXML, "</acceptors>", "</acceptors>\n" + divertConfig));

      if (TRACE_LOGS) {
         replaceLogs(ARTEMIS_INSTANCE);
      }
   }

   private static void replaceLogs(File serverLocation) throws Exception {
      File log4j = new File(serverLocation, "/etc/log4j2.properties");
      assertTrue(FileUtil.findReplace(log4j, "logger.artemis_utils.level=INFO", "logger.artemis_utils.level=INFO\n" + "\n" + "logger.divert.name=org.apache.activemq.artemis.core.server.impl.DivertImpl\nlogger.divert.level=TRACE"));
   }

   @BeforeEach
   public void startProcess() throws Exception {
      if (CREATE_SERVERS) {
         File dataDirectory = new File(ARTEMIS_INSTANCE, "./data");
         FileUtil.deleteDirectory(dataDirectory);
         serverProcess = ServerUtil.startServer("./target/myServer", "myServer");
         ServerUtil.waitForServerToStart(0, 5000);
      }
   }

   @AfterEach
   public void killProcess() throws Throwable {
      if (CREATE_SERVERS) {
         serverProcess.destroyForcibly();
         serverProcess.waitFor();
         serverProcess = null;
      }
   }

   @Test
   public void testConnections() throws Exception {
      int nElements = 1_000;
      OrdersIncomeRequest ordersIncomeRequest = new OrdersIncomeRequest();
      ordersIncomeRequest.setNumberOfOrders(nElements).setCommitInterval(100).setUri(ORDERS_INCOME_REQUEST_URI).setProtocol("CORE");

      BusinessProcessRequest businessProcessRequest = new BusinessProcessRequest();
      businessProcessRequest.setUri(BUSINESS_PROCESS_REQUEST_URI).setProtocol("CORE");

      businessProcessRequest.setElements(nElements).setConnections(10);

      BusinessService manufacturingRouteService = new BusinessService();
      manufacturingRouteService.startConnections(businessProcessRequest);

      IncomeService incomeService = new IncomeService();
      incomeService.process(ordersIncomeRequest);

      SimpleManagement simpleManagement = new SimpleManagement(SIMPLE_MANAGEMENT_URI, null, null);

      validateMessageCount(simpleManagement, ManufactureRouterBusiness.INCOME_ADDRESS, 0);
      validateMessageCount(simpleManagement, DeliveryRouteBusiness.OUTPUT_ADDRESS, nElements);

      manufacturingRouteService.closeConnections();
   }

   private static void validateMessageCount(SimpleManagement simpleManagement, String queue, int expectedCount) throws Exception {
      Waiter.waitFor(() -> {
         try {
            return simpleManagement.getMessageCountOnQueue(queue) == expectedCount;
         } catch (Exception e) {
            e.printStackTrace();
            return false;
         }
      }, TimeUnit.SECONDS, 300, TimeUnit.MILLISECONDS, 100);

      assertEquals(expectedCount, simpleManagement.getMessageCountOnQueue(queue));
   }
}
