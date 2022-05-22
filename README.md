# Telegramm Tinkoff Spring Boot Bot
Бот для торговли управляется через telegramm.  

# Getting Started
Перед запуском необходимо настроить ключ и токен.
В файл настройки /src/main/resources/application.properties
добавить переменные как в примере, но со своими значениями. 

Для Telegramm бота:
bot.username = my_telegram_name_bot
bot.token = my_telegram_token 

Для Tinkoff бота:
tinkoffbot.token = my_tinkoff_token 

# Run 
Для запуска проекта из консоли использйте команду:
    mvn spring-boot:run

# Bots command 
/help - помощь
/trade - начало торгов, создание аккаунта
/price - скачать свечи фиги
/orders - список ордеров
/stop - закрыть ордер
/start - запуск автоматической торговли
/done - остановка автоматической торговли
/buy - купить fifi
/sell - продать figi
/del - удалить аккаунт

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/maven-plugin/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)
* [Spring Data JPA](https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/reference/htmlsingle/#boot-features-jpa-and-spring-data)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Spring boot H2 Database example](https://java2blog.com/spring-boot-h2-database/)

### Default properties
* spring.datasource.url=jdbc:h2:mem:testdb
* spring.datasource.driverClassName=org.h2.Driver
* spring.datasource.username=sa
* spring.datasource.password=[no pass]
* spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

