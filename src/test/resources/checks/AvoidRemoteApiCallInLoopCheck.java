package checks;

import org.springframework.web.client.RestTemplate;

class AvoidRemoteApiCallInLoopCheck {

  void restTemplateInForLoop(RestTemplate client) {
    for (int i = 0; i < 3; i++) {
      client.getForObject("https://example.com", String.class); // Noncompliant {{Avoid remote API calls inside loops. Batch requests, cache data, or move calls outside loop to prevent N+1 network calls.}}
    }
  }

  void restTemplateInWhileLoop(RestTemplate client) {
    int i = 0;
    while (i++ < 2) {
      client.getForObject("https://example.com", String.class); // Noncompliant {{Avoid remote API calls inside loops. Batch requests, cache data, or move calls outside loop to prevent N+1 network calls.}}
    }
  }

  void outsideLoopIsAllowed(RestTemplate client) {
    client.getForObject("https://example.com", String.class);
  }
}
