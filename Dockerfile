# Start with a base image containing Maven (could also use openjdk:8-jdk-alpine here)
FROM amazoncorretto:17.0.11-alpine3.19

RUN apk add --no-cache maven

# Make project directory as working directory
RUN mkdir -p /app
WORKDIR /app

# Copy the pom.xml file
COPY pom.xml pom.xml

# This will download all maven dependencies (this step would not be needed if you are copying your .m2 folder into the image)
RUN mvn dependency:go-offline -B

# Copy your other files and build the project
COPY src src
RUN mvn clean install

# Create the directories
RUN mkdir -p transactions-export transactions-import

# Expose your application's default port
EXPOSE 8080

# Command to start the application
CMD ["java", "-cp", "target/banksy-1.0-SNAPSHOT.jar:target/lib/*", "org.jameshpark.banksy.MainKt"]