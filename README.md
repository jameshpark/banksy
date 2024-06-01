# Banksy
Banksy is a tool that normalizes and categories credit card and bank transactions. 
It ingests transactions from CSV files in the [transactions-import](transactions-import) directory,
normalizes the transactions into a single common schema, categorizes the transactions based on their descriptions,
and persists them in a local SQLite database.

Banksy can then export the transactions as a CSV file to the [transactions-export](transactions-export) directory and optionally
upload them directly to a specified Google Sheet.

## Getting Started
This is a [Kotlin](https://kotlinlang.org/docs/home.html) project and, as such, you'll need a modern version of a Kotlin compiler.
While you _can_ install the standalone compiler, I'd recommend installing IntelliJ IDEA instead for a few reasons:
1. IntelliJ IDEA supports Kotlin out of the box (installs and keeps up-to-date the Kotlin compiler)
2. Both IDEA and Kotlin are created/maintained by JetBrains
3. IDEA has the best support for Kotlin (intellisense, autosuggestions, project awareness) and it _will_ save you from shooting yourself in the foot
   1. In fact, IDEA may be the _only_ IDE/editor with reliable suggestions
4. There is no officially supported LSP for Kotlin, and the available unofficial LSPs are all incomplete AFAIK

### Prerequisites
* [JDK 17](https://adoptium.net/temurin/archive/?version=17)
* [Maven](https://maven.apache.org/)
* IntelliJ IDEA (includes Kotlin language compiler)

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
    2. Install IntelliJ IDEA (Community or Ultimate) through JetBrains Toolbox

### Local Development Setup
1. Copy `*.sample` files and add your own configs/secrets
    1. ```shell
       cp local.properties.sample local.properties
       cp local.merchants.json.sample local.merchants.json
       ```
    2. Specify values for these keys in `local.properties` if you want to export results to a Google Sheet
       ```shell
       google.client.secret.json=
       google.sheets.spreadsheet-id=
       google.sheets.spreadsheet-name=
       google.sheets.sheet-name=
       ```
    3. Add your own merchants to `local.merchants.json` to make Banksy useful for you
       ```shell
       [
          ...
         {
           "name": "American Airlines",
           "category": "VACATION",
           "regex": "american\\s?air?l?i?n?e?s?"
         },
         {
           "name": "Whole Foods",
           "category": "GROCERIES",
           "regex": "whole\\s?fo?o?ds?"
         },
         {
           "name": "some merchant you transacted with",
           "category": "a category defined in models/Category.kt (add more as you need)",
           "regex": "regular expression to identify these transactions"
         },
         ...
       ]
       ```
   
2. Build the project
   ```shell
   mvn clean install
   ```

3. Run it
   ```shell
   mvn exec:java
   ```
   1. You can also just click the green arrow next to `fun main()` in IntelliJ IDEA

Once built, Banksy can be run simply by calling
```
./banksy [--export-to-google-sheets=true/false, --help]
```
This is a convenient way to run Banksy when processing transactions and not actively developing/modifying the code.

### Steps to Process Transaction Data
1. Obtain your transaction data in CSV file(s).
2. Place your CSV file(s) in the [transactions-import](transactions-import) directory.
3. Add [Mapper](src/main/kotlin/org/jameshpark/banksy/transformer/Mapper.kt) instances to `enum class Mapper` and entries to the `headersToMappers` map as needed.
4. Run Banksy
5. Processed results will be saved to the [transactions-export](transactions-export) directory (and your Google Sheet if values are specified in `local.properties`).