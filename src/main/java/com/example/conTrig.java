package com.example;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;

//Configures the Tracer and exporter
public class conTrig {
    private static Tracer tracer = configureTracer();
    private static Meter meter;
    private static Tracer configureTracer() {
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:14250")
            .setTimeout(30, TimeUnit.SECONDS)
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
            .build();
        
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.builder(LoggingMetricExporter.create()).build())
        .build();

        // Set the tracer provider as the global tracer provider
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(sdkMeterProvider)
            .buildAndRegisterGlobal();
        

        meter = openTelemetry.meterBuilder("meter").build();
        // Get a tracer from the global tracer provider
        return openTelemetry.getTracer("tracer");
    }
    //Returns the open-telemtry meter
    public static Meter gMeter(){
        return meter;
    }
    //Returns the open-telemetry tracer
    public static Tracer getTracer(){
        return tracer;
    }
}
