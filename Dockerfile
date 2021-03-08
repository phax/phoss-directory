FROM tomcat:9-jdk11

ARG VERSION="0.9.4"
ARG WAR_NAME=phoss-directory-publisher-${VERSION}.war

WORKDIR $CATALINA_HOME/webapps

ENV CATALINS_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.security.egd=file:/dev/urandom"

RUN wget -O ${WAR_NAME} https://github.com/phax/phoss-directory/releases/download/phoss-directory-parent-pom-${VERSION}/${WAR_NAME}

RUN unzip ${WAR_NAME} -d ROOT && \
    rm -rf ${WAR_NAME}
