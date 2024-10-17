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

import java.util.Objects;

public class BaseRequest {
   String protocol;
   String uri;

   String user;

   String password;

   public String getProtocol() {
      return protocol;
   }

   public BaseRequest setProtocol(String protocol) {
      this.protocol = protocol;
      return this;
   }

   public String getUser() {
      return user;
   }

   public BaseRequest setUser(String user) {
      this.user = user;
      return this;
   }

   public String getPassword() {
      return password;
   }

   public BaseRequest setPassword(String password) {
      this.password = password;
      return this;
   }

   public String getUri() {
      return uri;
   }

   public BaseRequest setUri(String uri) {
      this.uri = uri;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      BaseRequest that = (BaseRequest) o;

      if (!Objects.equals(protocol, that.protocol))
         return false;
      return Objects.equals(uri, that.uri);
   }

   @Override
   public int hashCode() {
      int result = protocol != null ? protocol.hashCode() : 0;
      result = 31 * result + (uri != null ? uri.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "BaseRequest{" + "protocol='" + protocol + '\'' + ", uri='" + uri + '\'' + '}';
   }
}
