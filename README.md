# Banksy
Banksy is a tool that normalizes and categories credit card and bank transactions. 
It ingests transactions from CSV files in the [transactions-import](transactions-import) directory,
normalizes the transactions into a single common schema, categorizes the transactions based on their descriptions,
and persists them in a local SQLite database.

Banksy can then export the transactions as a CSV file to the [transactions-export](transactions-export) directory or 
upload them directly to a specified Google Sheet.

## Getting Started
This is a [Kotlin](https://kotlinlang.org/docs/home.html) project 

### Prerequisites
* [JDK 17](https://adoptium.net/temurin/archive/?version=17)
* [Maven](https://maven.apache.org/)
* IntelliJ IDEA (for programming Kotlin)

### macOS Setup
1. Install the JDK. I recommend using a version manager (I like [asdf](https://asdf-vm.com/)).
   ```shell
   asdf plugin add java
   asdf install java adoptopenjdk-17.0.9+9
   asdf local java adoptopenjdk-17.0.9+9
   ```

2. Install Maven (the build tool).
   ```shell
   brew install maven
   ```

3. Install IntelliJ IDEA (optional, but highly recommended, especially if you want to read/touch the code)
    1. ```shell
       brew install --cask jetbrains-toolbox
       ```
    2. Install IntelliJ IDEA (Community or Ultimate) through Jetbrains Toolbox

### Local Development Setup
1. Copy `*.sample` files and add your own configs/secrets
    1. ```shell
       cp local.properties.sample local.properties
       cp local.merchants.json.sample local.merchants.json
       ```
    2. Specify values for these keys in `local.properties`
       ```shell
       google.client.secret.json=
       google.sheets.spreadsheet-id=
       google.sheets.spreadsheet-name=
       google.sheets.sheet-name=
       ```
    3. Add your own merchants to `local.merchants.json` to make Banksy useful for you
       ```shell
       TODO
       ```
   
2. Build the project
   ```shell
   mvn clean install
   ```

3. Run it
   ```shell
   mvn exec:java
   ```

### Steps to Process Transaction Data
1. Obtain your transaction data in CSV file(s).
2. Place your CSV file(s) in the [transactions-import](transactions-import) directory.
3. Add [Mapper](src/main/kotlin/org/jameshpark/banksy/transformer/Mapper.kt) instances to `enum class Mapper` and entries to the `headersToMappers` map as needed.
4. Run Banksy
5. Processed results will be saved to the [transactions-export](transactions-export) directory and your Google Sheet.