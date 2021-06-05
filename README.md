# grpc-deadlock

demo of a deadlock problem in gRPC streaming server that I've encountered.


## BUILDING
```
mvn package
```

## RUNNING

Server:

```
java -jar target/grpc-deadlock-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Client:

```
java -cp target/grpc-deadlock-1.0-SNAPSHOT-jar-with-dependencies.jar pl.morgwai.samples.grpc.deadlock.EchoClient
```

## PROBLEM DESCRIPTION

Usually after few messages a deadlock occurs. Typical output looks like this:

Server:

> started gRPC EchoServer on port 6666  
> Jun 05, 2021 8:22:52 P.M. pl.morgwai.samples.grpc.deadlock.EchoService multiEcho  
> FINE: someone has just emitted an inconsiderated verbal vomit  
> Jun 05, 2021 8:22:53 P.M. pl.morgwai.samples.grpc.deadlock.EchoService multiEcho  
> FINER: sink clogged at rep 7  

Client:

> got echo  
> got echo  
> got echo  
> got echo  
> got echo  
> got echo  

...and both hang indefinitely.

If [line 27 in EchoServer](src/main/java/pl/morgwai/samples/grpc/deadlock/EchoServer.java#L27) and [lines 40](src/main/java/pl/morgwai/samples/grpc/deadlock/EchoService.java#L40) + [78 in EchoService](src/main/java/pl/morgwai/samples/grpc/deadlock/EchoService.java#L78) are uncommented, then everything works fine.

Posted [question to grpc group](https://groups.google.com/g/grpc-io/c/SFkHi1gvKx4) regarding this.
