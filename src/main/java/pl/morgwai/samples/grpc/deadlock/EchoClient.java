// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.samples.grpc.deadlock;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;



public class EchoClient {



	public static void main(String args[]) throws Exception {
		String target = "localhost:6666";
		if (args.length > 0) target = args[0];
		ManagedChannel channel = ManagedChannelBuilder
				.forTarget(target)
				.usePlaintext()
				.build();
		var connector = EchoServiceGrpc.newBlockingStub(channel);

		var request = EchoRequest
				.newBuilder()
				.setInconsideratedVerbalVomit(
						"bleeeeeeeeeeeeeeeeeeeehhhhhhhhhh" +
						"hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh")  // 64B
				.setReps(100)
				.build();
		var vomitIterator = connector.multiEcho(request);
		while (vomitIterator.hasNext()) {
			vomitIterator.next();
			System.out.println("got echo");
		}

		channel.shutdown().awaitTermination(100, TimeUnit.MILLISECONDS);
		if ( ! channel.isTerminated()) {
			System.out.println("channel has NOT shutdown cleanly");
		}
	}
}
