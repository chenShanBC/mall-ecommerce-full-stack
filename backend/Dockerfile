FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./
COPY mall-common/pom.xml mall-common/
COPY mall-auth/pom.xml mall-auth/
COPY mall-user/pom.xml mall-user/
COPY mall-product/pom.xml mall-product/
COPY mall-cart/pom.xml mall-cart/
COPY mall-order/pom.xml mall-order/
COPY mall-stock/pom.xml mall-stock/
COPY mall-pay/pom.xml mall-pay/
COPY mall-admin/pom.xml mall-admin/
COPY mall-file/pom.xml mall-file/
COPY mall-aftersale/pom.xml mall-aftersale/
COPY mall-start/pom.xml mall-start/

RUN mvn -pl mall-start -am dependency:go-offline

COPY . .
RUN mvn -pl mall-start -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/mall-start/target/mall-start-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090

ENV SPRING_PROFILES_ACTIVE=cloud
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
