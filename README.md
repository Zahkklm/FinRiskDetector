# FinRisk Detector

## Overview

The FinRisk Detector is a comprehensive financial risk management system built with Spring Boot. It provides tools for assessing transaction risks, managing portfolios, and monitoring market activities through both a REST API and an interactive web interface.

## Features

* **Risk Assessment:** Evaluate the risk associated with transactions and user profiles.
* **Transaction Monitoring:** Track and analyze transactions in real-time.
* **Anomaly Detection:** Identify unusual patterns in transaction data.
* **Portfolio Management:** Track asset holdings and manage cash balances.
* **Market Simulation:** Simulated market data for testing and development.
* **Trading Platform:** Execute buy/sell orders on various assets.
* **Interactive Dashboard:** Web-based UI for visualizing market data and portfolio performance.

## Technologies Used

### Backend:
* Java 17
* Spring Boot
* JPA/Hibernate
* MySQL
* Redis for caching
* Kafka for messaging

### Frontend:
* HTML5
* CSS3
* JavaScript
* Bootstrap 5
* Chart.js for data visualization
* Axios for API requests

### Documentation:
* Swagger/OpenAPI 3.0
* JavaDoc

## Project Structure
```
finrisk-detector
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/riskengine/risksystem
│   │   │       ├── FinRiskDetectorApplication.java
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/                  # Data Transfer Objects
│   │   │       ├── market/               # Market simulation components
│   │   │       │   ├── model/
│   │   │       │   ├── service/
│   │   │       │   └── simulation/
│   │   │       ├── model/                # Domain models
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── resources
│   │       ├── application.properties
│   │       └── static/                   # Frontend resources
│   │           ├── index.html
│   │           ├── css/
│   │           │   └── styles.css
│   │           └── js/
│   │               └── app.js
│   └── test
│       └── java/com/riskengine/risksystem/
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Setup Instructions

### Prerequisites

* Java JDK 17+
* Maven
* Docker (optional, for containerized deployment)

### Running the Application

1.  **Clone the Repository:**
    ```bash
    git clone <repository-url>
    cd finrisk-detector
    ```

2.  **Build the Project:**
    ```bash
    mvn clean install
    ```

3.  **Run with Maven:**
    ```bash
    mvn spring-boot:run
    ```

4.  **Alternative: Run with Docker:**
    ```bash
    docker-compose up
    ```

## Usage

### Web Interface

Access the web dashboard at:
[http://localhost:8080](http://localhost:8080)

The dashboard provides:
* Market view with price charts
* Portfolio management
* Trading interface
* Risk analysis

### API Documentation

Access the Swagger UI at:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Key API Endpoints

#### Market Operations
* `GET /api/market/prices` - Get all current market prices
* `GET /api/market/portfolio/{userId}` - Get user portfolio
* `POST /api/market/order` - Place trading order

#### Risk Assessment
* `POST /api/risk-assessment/evaluate` - Evaluate risk for existing transaction
* `POST /api/risk-assessment/evaluate-transaction` - Evaluate risk for new transaction

#### Transaction Monitoring
* `GET /api/transactions` - List all transactions
* `GET /api/transactions/{id}` - Get transaction details

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.
