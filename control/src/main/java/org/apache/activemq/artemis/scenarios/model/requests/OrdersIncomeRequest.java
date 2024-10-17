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

package org.apache.activemq.artemis.scenarios.model.requests;

public class OrdersIncomeRequest extends BaseRequest {
   int numberOfOrders;
   int commitInterval;

   public int getNumberOfOrders() {
      return numberOfOrders;
   }

   public OrdersIncomeRequest setNumberOfOrders(int numberOfOrders) {
      this.numberOfOrders = numberOfOrders;
      return this;
   }

   public int getCommitInterval() {
      return commitInterval;
   }

   public OrdersIncomeRequest setCommitInterval(int commitInterval) {
      this.commitInterval = commitInterval;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      if (!super.equals(o))
         return false;

      OrdersIncomeRequest that = (OrdersIncomeRequest) o;

      if (numberOfOrders != that.numberOfOrders)
         return false;
      return commitInterval == that.commitInterval;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + numberOfOrders;
      result = 31 * result + commitInterval;
      return result;
   }
}
