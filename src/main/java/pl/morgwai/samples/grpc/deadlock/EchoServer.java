// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.samples.grpc.deadlock;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;



public class EchoServer {



	Server echoServer;



	public void startAndAwaitTermination(int port)
			throws IOException, InterruptedException {
		echoServer = NettyServerBuilder
			.forPort(port)
			.maxConnectionAge(10, TimeUnit.MINUTES)
			.maxConnectionAgeGrace(12, TimeUnit.HOURS)
			.addService(new EchoService())
//			.directExecutor()
			.build();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		echoServer.start();
		System.out.println("started gRPC EchoServer on port " + port);
		echoServer.awaitTermination();
	}



	Thread shutdownHook = new Thread(() -> {
		try {
			echoServer.shutdown().awaitTermination(5, TimeUnit.SECONDS);
			if (echoServer.isTerminated()) {
				System.out.println("shutdown completed");
			} else {
				System.err.println("server has failed to shutdown cleanly");
			}
		} catch (InterruptedException e) {}
	});



	public static void main(String args[]) throws Exception {
		new EchoServer().startAndAwaitTermination(6666);
	}
}
