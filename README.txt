The projects uses Maven and docker, so be sure to have those downloaded. In addition, make sure to run 
"maven clean install" to make sure the dependencies are downloaded

To Execute:

1. Run the random.js file to generate 20 input files
2. Run the server.js file to start server
3. Run Jaegar all-in-one to veiw openTelemetry data 
4. Open Jaegar on http://localhost:16686/
5. Run the client.js file to start client

Execution Commands:
mvn exec:java -Dexec.mainClass="com.example.server"

mvn exec:java -Dexec.mainClass="com.example.client"

mvn exec:java -Dexec.mainClass="com.example.random"

docker run --rm -p 16686:16686 -p 6831:6831/udp -p 6832:6832/udp jaegertracing/all-in-one:latest
