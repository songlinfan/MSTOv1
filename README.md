# MSTO

MSTO enables secure queries over inverted indexes in the SSE setting, while hiding access patterns, search patterns, size patterns, and operation-type patterns. Additionally, MSTO provides a general method for securely performing queries on multi-maps.



This repository contains the implementation of MSTO, along with comprehensive instructions for reproducing the results presented in our paper.

## 1. Overview

We now offer a detailed overview of the repository’s structure along with descriptions of the purposes of its core components.

### 1.1. Repository structure

```
MSTOv1/
├── emails_raw/              # Raw email data
├── emails_processer/        # Email preprocessing tools
├── emails_processed/        # Cleaned and processed email data
├── inverted_index/       	 # Constructed inverted indexes (datasets to be used)
├── data/                    # Contains client-side and server-side data
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── MST/                         # Multi-Stacked Tree core classes
│   │   │   ├── common/                      # Shared utility classes
│   │   │   ├── benchmark_ciphertext/        # Encrypted MST construction and queries
│   │   │   ├── benchmark_plaintext/         # Plaintext MST construction and queries
│   │   └── resources/                       # Configuration files
│   └── test/                                # Unit tests
├── pom.xml                 # Maven project configuration
└── README.md               # Project documentation
```



### 1.2. Email dataset preparation

We now briefly outline the process of extracting data from emails and constructing the corresponding inverted index. (For detailed commands on dataset construction, see **Section 2.2.**)

1. All raw email files should initially be placed in the `emails_raw/` directory.

2. Parse the Enron emails.

   1. `emails_processer/keyword_extractor.py` provides a method to extract keywords from emails. It processes all raw emails stored in `emails_raw/` and outputs the results to `emails_processed/` in the form of JSON files (each named by the email's UUID).   

      ```
      ├── emails_processed/
      │   ├── 0000074f-96de-4403-b6e1-7d2c0153dd87.json
      │   ├── 73ff0056-7f8c-480c-a102-7e0b1cf38c98.json
      │   ├──                 ...
      ```

      Each JSON file follows the format below:

      ```json
      {
        "email_path": "...",
        "uuid": "0000074f-96de-4403-b6e1-7d2c0153dd87",
        "keywords": [
          ...
         ]
      }
      ```

   2. `inverted_index/inverted_index_generator.py` processes all JSON files in the `emails_processed/` directory and generates a single `inverted_index.json` file representing the entire Enron dataset.

   3. To measure the size of different datasets, `inverted_index/inverted_index_splitter.py` provides a method to split the inverted index. It generates one JSON file per keyword in the following format, and saves them in the `inverted_index/inverted_index_split/` directory. Each file is named according to the keyword’s index number:

      ```json
      5.json:  											// Represents the 5th keyword processed
      { "hotel": [                                        // keyword
              "06961088-cfb7-4da5-bab8-c75a5ca38299",		// UUID list	
              "0527c20f-aaef-4013-83f1-7d0c0b0854d8",
          				... 
      		]
      }
      ```

Overall, each directory under `inverted_index/` represents a dataset used in our experiments. In each dataset, every keyword corresponds to a JSON file that contains the sequence of document IDs associated with that keyword.



### 1.3. Ethical concerns

We conduct our experiments using the Enron email corpus, which consists of real email communications from Enron employees. The dataset is utilized solely for evaluating the performance of MSTO under realistic workload conditions. There is no intention to compromise the privacy of individuals mentioned in the emails. We kindly ask users of the dataset to respect the privacy of those involved.



### 1.4. Benchmark Ciphertext: MST Setup and Query

1. The process of MST setup and query is the same for both plaintext and ciphertext benchmarks. Here, we use the ciphertext case as an example.

   The structure of the `benchmark_ciphertext/` directory is as follows:

   ```
   benchmark_ciphertext/
   ├── client/
   │   ├── ClientSetup.java  
   │   ├── ClientQuery.java
   │   └── ClientShutdown.java  
   └── server/
       ├── ServerSetup.java  
       └── ServerResponse.java
   ```

   1. `ClientSetup.java` and `ServerSetup.java` simulate the interaction between the client and server during MST setup:
      - The client generates the MST instance locally and uploads it to the server.
      - The server receives the instance and stores it locally.
   2. `ClientQuery.java` and `ServerResponse.java` simulate the interaction during query processing:
      - The client sends a query keyword to the server.
      - The server responds with all buckets along the corresponding path.
      - The client extracts the target document locally, updates all received buckets, and sends them back to the server.
   3. `ClientShutdown.java` allows the client to proactively terminate the connection with the server.





## 2. Getting Started

This section provides step-by-step instructions for constructing the dataset, as well as building, configuring, and running the MSTO project from source.

### 2.1. Environment setup

To successfully build and run this project, please ensure the following environment dependencies are properly installed:

Required Tools and Versions：

- Java: JDK 17：[Java Archive Downloads - Java SE 17.0.12 and earlier](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- Maven 3.8.9：[Download Apache Maven – Maven](https://maven.apache.org/download.cgi)
- Python 3.8：[Python Release Python 3.8.0 | Python.org](https://www.python.org/downloads/release/python-380/)

After installing all required tools, run the following commands to verify each component is correctly installed and accessible from the command line:

- `java -version`
  java version "17.0.12" 2024-07-16 LTS
  Java(TM) SE Runtime Environment (build 17.0.12+8-LTS-286)
- `mvn -v`
  Apache Maven 3.8.9 
  Java version: 17.0.12, vendor: Oracle Corporation
- `python --version`
  Python 3.8.0



### 2.2. Preparing the dataset

We have provided the three datasets referenced in the paper under `inverted_index/dataset1/`, `inverted_index/dataset2/`, and `inverted_index/dataset3/`.
If you wish to directly evaluate MSTO using the provided datasets, you may skip this subsection.
Otherwise, please follow the instructions below to prepare your own dataset：

1. Download the [Enron Email Dataset](https://www.cs.cmu.edu/~enron/), unzip the emails, and place the extracted emails into the `emails_raw/` directory.

   The resulting directory structure should be as follows:

   ```
   ├── emails_raw/
   │   ├── maildir
   │   │   ├── allen-p
   │   │   ├── arnold-j
   ```

2. Install the Python package `nltk` by running:

   ```
   pip install nltk
   ```

3. Navigate to the `emails_processer` directory and run:

   ```
   python keyword_extractor.py
   ```

   This process may take some time because it processes the entire Enron dataset. After processing each file, a JSON file will be generated in the `emails_processed/` directory.

4. Navigate to the `inverted_index/` directory and run:

   ```
   python inverted_index_generator.py
   ```

   This script extracts keywords and UUID mappings from all JSON files in `emails_processed/` and generates an inverted index saved as `./inverted_index.json`.

5. Run:

   ```
   python inverted_index_splitter.py
   ```

   This splits the `inverted_index.json` file and outputs the results into the `inverted_index_split` directory, which serves as the final dataset ready for use



### 2.3. Customizing MST Construction via Config File

After constructing the dataset, you can modify the dataset selection and specify the number of keywords to be used for MST construction in `src/main/resources/config.properties`. We now provide an explanation of the parameters you should modify to help you customize the program.

```properties
# Dataset configuration:

# Path to the dataset directory used by the inverted index.
# This should be a subdirectory under inverted_index/
# You can select different datasets here as the target to be processed.
PathOfDatasetUsed = inverted_index/dataset1

# Number of keywords to process from the dataset.
# Set to -1 to process the entire dataset.
numOfKeywords = -1


# Maximum number of blocks that can be stored in each bucket.
# This corresponds to parameter Z in the paper.
bucketCapacity = 5
```



### 2.4. Project Build

1. Navigate to the `MSTOv1` directory and run the following command:

   ```
    mvn clean package
   ```

​	This command will:

​		Clean any previous builds

​		Compile the source code

​		Package the code along with all dependencies into a runnable JAR file

2. After a successful build, you should see output like:

   ```bash
   [INFO] BUILD SUCCESS
   ```

3. The compiled JAR file will be located at:

   ```
   target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

After the JAR is created, you can execute the program using this generated file.



> **Note:** Each time you modify the configuration file, you must re-build the project using the command above.





### 2.5. Project Execution

Before running the project, you can configure the dataset and bucket capacity by modifying the following parameters in `src/main/resources/config.properties`.

```properties
# Example
# Following parameters mean that the entire dataset located at "inverted_index/dataset3" will be processed
PathOfDatasetUsed = inverted_index/dataset3
numOfKeywords = -1
```

Now, we proceed with the formal construction and query of our MST. We provide both plaintext and ciphertext versions for construction and querying.



All of the following commands must be run from the root directory of the `MSTOv1` project.

### 2.4.1 Ciphertext Benchmark

------

#### Step 1: Generate the Secret Key

Open a terminal and run the following command:

```
java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar common.SecretKeyGenerator
```

This will generate the secret key required by the client and save it under: `data/client/ciphertext/`.


#### Step 2: Build the EMST (Encrypted Multi-Stacked Tree)

1. In the **first terminal** (server side), start the server:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_ciphertext.server.ServerSetup
   ```

   The server will listen on port `server_port` (default `8888`) and wait for the client to send the EMST instance.

   > Once the server receives the MST instance from the client, it will store it in the path specified by `EMSTFilePath`.

2. Open a **second terminal** (client side), and run the client setup:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_ciphertext.client.ClientSetup
   ```

   The client will collect your selected dataset and keyword count, generate the inverted index, and construct a Encrypted  Multi-Stacked Tree (EMST) that encapsulates the entire index. It will then send the MST instance to the server.

At this point, the MST instance generated from your dataset has been successfully stored on the server. Both terminals are now free for running queries.

------

#### Step 3: Perform Keyword Queries

Use the same two terminals from above.

1. On the **server side**, run:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_ciphertext.server.ServerResponse
   ```

   This command loads the stored EMST instance into memory so it can respond to client queries.

2. On the **client side**, run:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_ciphertext.client.ClientQuery <your_keyword>
   ```

   The client sends the leaf ID corresponding to the queried keyword to the server. 

   The server responds with the path of encrypted buckets.

   The client decrypts and parses each block in the returned buckets, extracts the target UUIDs, and saves them to `CipherSearchResultFilePath`.

   It then updates the modified buckets locally, evicts them back to the server, and assigns a new leaf for the queried keyword.

   > You can repeat this step as many times as you wish, using different or repeated keywords, to observe communication patterns.

------

#### Step 4: Shutdown

When you're done querying, shut down the client by running:

```bash
java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_ciphertext.client.ClientShutdown
```

This command informs the server to gracefully terminate the session and write the updated EMST instance back to persistent storage.

> **Do not force-close the server terminal.**  
> You should shut down the server gracefully by using `ClientShutdown`, or wait for the server's configured timeout to disconnect automatically.  
> If you terminate the server forcefully, the updated EMST stored in memory will not be written back to disk, which may lead to incorrect or failed queries later on.



### 2.4.2 Plaintext Benchmark

------

####  Step 1: Build the MST (Multi-Stacked Tree)

1. In the **first terminal (server side)**, start the server:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_plaintext.server.ServerSetup
   ```

   The server will listen on port `server_port` (default `8888`) and wait for the client to send the MST instance.

   > Once the server receives the MST instance from the client, it will store it in the path specified by `MSTFilePath`.

2. Open a **second terminal (client side)**, and run the client setup:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_plaintext.client.ClientSetup
   ```

   The client will generate the inverted index based on your selected dataset and specified number of keywords, and construct a Multi-Stacked Tree (MST) that encapsulates the entire index. It will then send the MST instance to the server.

At this point, the MST instance generated from your dataset has been successfully stored on the server. Both terminals are now free for running queries.

------

#### Step 2: Perform Keyword Queries

Use the same two terminals from above.

1. On the **server side**, run:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_plaintext.server.ServerResponse
   ```

   This command loads the stored MST instance into memory so it can respond to client queries.

2. On the **client side**, run:

   ```bash
   java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_plaintext.client.ClientQuery <your_keyword>
   ```

   The client sends the leaf ID corresponding to the queried keyword to the server. 

   The server responds with the path of encrypted buckets.

   The client decrypts and parses each block in the returned buckets, extracts the target UUIDs, and saves them to `PlainSearchResultFilePath`.

   It then updates the modified buckets locally, evicts them back to the server, and assigns a new leaf for the queried keyword.

   > You can repeat this step as many times as you wish, using different or repeated keywords, to observe communication patterns.

------

#### Step 3: Shutdown

When you're done querying, shut down the client by running:

```bash
java -cp target/MSTOv1-1.0-SNAPSHOT-jar-with-dependencies.jar benchmark_plaintext.client.ClientShutdown
```

This command informs the server to gracefully terminate the session and write the updated MST instance back to persistent storage.

> **Do not force-close the server terminal.**  
> You should shut down the server gracefully by using `ClientShutdown`, or wait for the server's configured timeout `server_timeout` to disconnect automatically.  
> If you terminate the server forcefully, the updated MST stored in memory will not be written back to disk, which may lead to incorrect or failed queries later on.



## 3. Experimental Results

After successfully executing the steps above, you can observe the components involved in the execution and output of the project under the `data/client` and `data/server` directories, respectively. The components generated for the ciphertext benchmark are listed as follows:

```
data/client/ciphertext/
├── KeywordToLeaf.json              # Mapping of keywords to leaf node indices
├── SearchResult.txt                # Search results for queried keywords
├── SecretKey.txt                   # Secret key used for encryption
└── stash.ser                       # Serialized stash file

data/server/ciphertext/
├── CiphertextCommunicationRecord.log   # Communication logs recorded by the server
└── emst.ser                            # Serialized EMST (Encrypted Multi-Stacked Tree) instance
```



# 4. Known issues 

1. In this version of MSTO, the entire EMST is treated as a single unit, and must be fully loaded into memory during query execution. However, an alternative approach—loading only the buckets relevant to a given query from storage—can be adopted. This optimized approach will be introduced in MSTO v2.
2. The configuration file is located in `src/main/resources`, and it is bundled into the JAR during the Maven build process. Therefore, any changes to the configuration parameters require rebuilding the project. However, if you are running the Maven project within an IDE (such as IntelliJ IDEA or Eclipse), rather than executing the JAR file directly, the parameters can be modified and applied in real time without rebuilding.
3. For simplicity, the same dummy block is used to fill all padding positions. These blocks serve purely as placeholders and do not affect the correctness of the queries. In a real-world deployment, the dummy blocks can be replaced with varied filler content as needed.
4. The communication between the client and the server is currently not optimized. In the current implementation, each key-value pair is transmitted separately. As a result, the encrypted EMST may sometimes appear to transmit faster than the plaintext EMST during experiments. This is an implementation artifact, and communication efficiency should not be considered a factor in evaluating the core performance of the scheme.













