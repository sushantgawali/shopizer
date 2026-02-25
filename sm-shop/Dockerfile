FROM eclipse-temurin:11-jre
RUN mkdir -p /opt/app /files
WORKDIR /opt/app
COPY target/shopizer.jar shopizer.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /opt/app/shopizer.jar"]
