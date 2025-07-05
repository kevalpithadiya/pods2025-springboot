FROM eclipse-temurin:21-alpine

COPY ./h2* /h2.jar

ENTRYPOINT [ "java", "-cp", "./h2.jar", "org.h2.tools.Server", "-tcp", "-tcpAllowOthers", "-tcpPort", "9082", "-ifNotExists" ]
