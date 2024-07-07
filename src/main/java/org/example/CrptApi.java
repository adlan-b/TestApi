package org.example;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

    public class CrptApi {
        private final Semaphore semaphore;
        private final ScheduledExecutorService scheduler;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        public CrptApi(TimeUnit timeUnit, int requestLimit) {
            this.semaphore = new Semaphore(requestLimit, true);
            this.scheduler = Executors.newScheduledThreadPool(1);
            this.httpClient = HttpClient.newHttpClient();
            this.objectMapper = new ObjectMapper();

            long period = timeUnit.toMillis(1);
            scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()), period, period, TimeUnit.MILLISECONDS);
        }

        public void createDocument(Document document, String signature) throws Exception {
            semaphore.acquire();

            String jsonBody = objectMapper.writeValueAsString(document);
            System.out.println(jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to create document: " + response.body());
            }

        }

        public void shutdown() {
            scheduler.shutdown();
        }

        public static class Document {
            public Description description;
            public String doc_id;
            public String doc_status;
            public String doc_type = "LP_INTRODUCE_GOODS";
            public boolean importRequest;
            public String owner_inn;
            public String participant_inn;
            public String producer_inn;
            public String production_date;
            public String production_type;
            public Product[] products;
            public String reg_date;
            public String reg_number;

            public static class Description {
                public String participantInn;
            }

            public static class Product {
                public String certificate_document;
                public String certificate_document_date;
                public String certificate_document_number;
                public String owner_inn;
                public String producer_inn;
                public String production_date;
                public String tnved_code;
                public String uit_code;
                public String uitu_code;
            }
        }


    }

