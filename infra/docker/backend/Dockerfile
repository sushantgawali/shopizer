FROM eclipse-temurin:11-jre-focal
WORKDIR /app
# shopizer.jar is fetched from GitHub Releases and placed here by the Makefile
COPY shopizer.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "shopizer.jar"]
