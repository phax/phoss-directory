#
# Copyright (C) 2015-2025 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM tomcat:9-jdk11

ARG VERSION="0.9.9"
ARG WAR_NAME=phoss-directory-publisher-${VERSION}.war

WORKDIR $CATALINA_HOME/webapps

ENV CATALINS_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/urandom"

RUN wget -O ${WAR_NAME} https://github.com/phax/phoss-directory/releases/download/phoss-directory-parent-pom-${VERSION}/${WAR_NAME}

RUN unzip ${WAR_NAME} -d ROOT && \
    rm -rf ${WAR_NAME}
